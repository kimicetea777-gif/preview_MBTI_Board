package com.board.kimicetea.global.security.session;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final UUID AUTH_SERVER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String DEVICE_ID = "device-001";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(redisTemplate, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(sessionService, "sessionTtlDays", 30L);
    }

    private User user(final UserRole role, final UserState state) {
        return User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .authServerId(AUTH_SERVER_ID)
                .nickname("nick")
                .mbtiVerified(MbtiVerified.DEFAULT)
                .role(role)
                .state(state)
                .build();
    }

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("SESS/SESSDATA 두 키를 모두 TTL과 함께 저장하고 role/blocked를 로컬 User 상태로 채운다")
        void createsBothKeys() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            final String sessionId = sessionService.createSession(user(UserRole.USER, UserState.NORMAL), DEVICE_ID, "id-token-value");

            final ArgumentCaptor<String> sessDataJson = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(eq("SESSDATA:" + sessionId), sessDataJson.capture(), eq(Duration.ofDays(30)));
            verify(valueOperations).set(eq("SESS:" + AUTH_SERVER_ID + ":" + DEVICE_ID), eq(sessionId), eq(Duration.ofDays(30)));

            assertThat(sessDataJson.getValue()).contains("\"role\":\"USER\"").contains("\"blocked\":false");
        }

        @Test
        @DisplayName("차단 상태(WITHDRAWN)의 유저는 blocked=true로 저장된다")
        void blockedStateStoresTrue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            final String sessionId = sessionService.createSession(user(UserRole.USER, UserState.WITHDRAWN), DEVICE_ID, "id-token");

            final ArgumentCaptor<String> sessDataJson = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(eq("SESSDATA:" + sessionId), sessDataJson.capture(), any(Duration.class));
            assertThat(sessDataJson.getValue()).contains("\"blocked\":true");
        }
    }

    @Nested
    @DisplayName("getSessionData")
    class GetSessionData {

        @Test
        @DisplayName("저장된 세션을 라운드트립으로 복원한다")
        void roundTrips() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            final String sessionId = sessionService.createSession(user(UserRole.ADMIN, UserState.NORMAL), DEVICE_ID, "id-token");

            final ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(eq("SESSDATA:" + sessionId), jsonCaptor.capture(), any(Duration.class));
            when(valueOperations.get("SESSDATA:" + sessionId)).thenReturn(jsonCaptor.getValue());

            final Optional<SessionService.SessionData> result = sessionService.getSessionData(sessionId);

            assertThat(result).isPresent();
            assertThat(result.get().userId()).isEqualTo(USER_ID);
            assertThat(result.get().authServerId()).isEqualTo(AUTH_SERVER_ID);
            assertThat(result.get().role()).isEqualTo("ADMIN");
            assertThat(result.get().deviceId()).isEqualTo(DEVICE_ID);
            assertThat(result.get().idToken()).isEqualTo("id-token");
            assertThat(result.get().blocked()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 세션이면 빈 Optional을 반환한다")
        void missingReturnsEmpty() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            assertThat(sessionService.getSessionData("no-such-session")).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateRoleAndBlocked")
    class UpdateRoleAndBlocked {

        @Test
        @DisplayName("role/blocked만 갱신하고 나머지 필드는 유지하며, TTL을 다시 세팅한다(슬라이딩)")
        void updatesOnlyRoleAndBlocked() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            final SessionService.SessionData current = new SessionService.SessionData(
                    USER_ID, AUTH_SERVER_ID, "user@example.com", DEVICE_ID, "id-token", "USER", false);

            sessionService.updateRoleAndBlocked("session-1", current, "ADMIN", true);

            final ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(eq("SESSDATA:session-1"), jsonCaptor.capture(), eq(Duration.ofDays(30)));
            assertThat(jsonCaptor.getValue()).contains("\"role\":\"ADMIN\"").contains("\"blocked\":true")
                    .contains("\"deviceId\":\"" + DEVICE_ID + "\"");
        }

        @Test
        @DisplayName("SESS 역방향 인덱스의 TTL도 같이 슬라이딩한다 — 안 그러면 SESSDATA만 계속 살아있고 " +
                "deleteAllSessions()의 SCAN이 이 세션을 못 찾게 됨")
        void alsoSlidesReverseIndexTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            final SessionService.SessionData current = new SessionService.SessionData(
                    USER_ID, AUTH_SERVER_ID, "user@example.com", DEVICE_ID, "id-token", "USER", false);

            sessionService.updateRoleAndBlocked("session-1", current, "ADMIN", true);

            verify(valueOperations).set(
                    eq("SESS:" + AUTH_SERVER_ID + ":" + DEVICE_ID), eq("session-1"), eq(Duration.ofDays(30)));
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("세션 데이터를 조회해 SESS/SESSDATA 두 키를 모두 삭제한다")
        void deletesBothKeys() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            final String json = """
                    {"userId":"%s","authServerId":"%s","email":"user@example.com","deviceId":"%s","idToken":"id-token","role":"USER","blocked":false}
                    """.formatted(USER_ID, AUTH_SERVER_ID, DEVICE_ID).strip();
            when(valueOperations.get("SESSDATA:session-1")).thenReturn(json);

            sessionService.deleteSession("session-1");

            verify(redisTemplate).delete("SESS:" + AUTH_SERVER_ID + ":" + DEVICE_ID);
            verify(redisTemplate).delete("SESSDATA:session-1");
        }

        @Test
        @DisplayName("이미 없는 세션이면 아무것도 삭제하지 않는다")
        void noOpWhenMissing() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("SESSDATA:missing")).thenReturn(null);

            sessionService.deleteSession("missing");

            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("deleteAllSessions")
    class DeleteAllSessions {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("해당 authServerId의 모든 SESS 키를 스캔해서 SESSDATA와 함께 일괄 삭제한다")
        void scansAndDeletesAll() {
            final List<String> sessKeys = List.of(
                    "SESS:" + AUTH_SERVER_ID + ":device-1",
                    "SESS:" + AUTH_SERVER_ID + ":device-2");
            // Cursor는 목 대상이라 Iterator의 default 메서드(forEachRemaining)가 실제 실행되지 않으므로 직접 스텁
            final Cursor<String> cursor = mock(Cursor.class);
            doAnswer(invocation -> {
                final java.util.function.Consumer<String> action = invocation.getArgument(0);
                sessKeys.forEach(action);
                return null;
            }).when(cursor).forEachRemaining(any());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.multiGet(sessKeys)).thenReturn(List.of("session-a", "session-b"));

            sessionService.deleteAllSessions(AUTH_SERVER_ID);

            verify(redisTemplate).delete(List.of("SESSDATA:session-a", "SESSDATA:session-b"));
            verify(redisTemplate).delete(sessKeys);
        }

        @Test
        @DisplayName("스캔 결과가 없으면 delete를 호출하지 않는다")
        @SuppressWarnings("unchecked")
        void noOpWhenNoKeysFound() {
            // forEachRemaining을 스텁하지 않으면 목은 아무 것도 순회하지 않음 — sessKeys가 비어있는 케이스와 동일
            final Cursor<String> cursor = mock(Cursor.class);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            sessionService.deleteAllSessions(AUTH_SERVER_ID);

            verify(redisTemplate, never()).delete(anyList());
            verify(redisTemplate, never()).delete(anyString());
        }
    }
}
