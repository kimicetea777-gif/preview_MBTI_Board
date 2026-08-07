package com.board.kimicetea.global.security.session;

import com.board.kimicetea.domain.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * AS(Authorization Server) 로그인 기반 서버 세션. 브라우저는 opaque sessionId 쿠키만 들고 있고,
 * 실제 인증 상태(로컬 userId/role/차단여부, AS id_token)는 전부 Redis에만 보관한다.
 * Key: SESS:{authServerId}:{deviceId} → sessionId, SESSDATA:{sessionId} → SessionData.
 * TTL은 AS refresh token 수명(설정값)과 맞추고, save할 때마다 슬라이딩된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String SESS_PREFIX = "SESS:";
    private static final String SESSDATA_PREFIX = "SESSDATA:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${authserver.refresh-token-ttl-days:30}")
    private long sessionTtlDays;

    /** 로그인 성공 시 세션 부트스트랩 — role/blocked는 이 시점 로컬 User 상태로 1회 채움 */
    public String createSession(final User user, final String deviceId, final String idToken) {
        final String sessionId = UUID.randomUUID().toString();
        final SessionData data = new SessionData(
                user.getId(),
                user.getAuthServerId(),
                user.getEmail(),
                deviceId,
                idToken,
                user.getRole().name(),
                user.getState().isBlocked());
        writeSessionData(sessionId, data);
        redisTemplate.opsForValue().set(sessKey(user.getAuthServerId(), deviceId), sessionId, ttl());
        return sessionId;
    }

    public Optional<SessionData> getSessionData(final String sessionId) {
        final String json = redisTemplate.opsForValue().get(sessDataKey(sessionId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, SessionData.class));
        } catch (final JsonProcessingException e) {
            log.error("세션 데이터 역직렬화 실패 (sessionId={})", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * AS access token이 실제로 refresh될 때만 호출 — role/blocked만 로컬 User 재조회 값으로 갱신하고
     * TTL도 같이 슬라이딩한다. SESS:{authServerId}:{deviceId} 역방향 인덱스의 TTL도 반드시 같이
     * 슬라이딩해야 함 — 안 그러면 SESSDATA는 계속 갱신돼 살아있는데 SESS만 먼저 만료되어
     * deleteAllSessions()의 SCAN이 이 세션을 못 찾게 된다(강제 로그아웃 누락으로 이어짐).
     */
    public void updateRoleAndBlocked(final String sessionId, final SessionData current, final String role, final boolean blocked) {
        writeSessionData(sessionId, new SessionData(
                current.userId(), current.authServerId(), current.email(), current.deviceId(), current.idToken(),
                role, blocked));
        redisTemplate.opsForValue().set(sessKey(current.authServerId(), current.deviceId()), sessionId, ttl());
    }

    /** 단일 기기 로그아웃 */
    public void deleteSession(final String sessionId) {
        getSessionData(sessionId).ifPresent(data -> {
            redisTemplate.delete(sessKey(data.authServerId(), data.deviceId()));
            redisTemplate.delete(sessDataKey(sessionId));
        });
    }

    /** 전체 기기 강제 로그아웃 — 관리자 강제탈퇴, 자진탈퇴, RT 재사용 감지 등에서 호출 */
    public void deleteAllSessions(final UUID authServerId) {
        final String pattern = SESS_PREFIX + authServerId + ":*";
        final List<String> sessKeys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(sessKeys::add);
        }
        if (sessKeys.isEmpty()) {
            return;
        }

        final List<String> sessionIds = redisTemplate.opsForValue().multiGet(sessKeys);
        if (sessionIds != null) {
            final List<String> sessDataKeys = sessionIds.stream()
                    .filter(Objects::nonNull)
                    .map(this::sessDataKey)
                    .toList();
            if (!sessDataKeys.isEmpty()) {
                redisTemplate.delete(sessDataKeys);
            }
        }
        redisTemplate.delete(sessKeys);
    }

    private void writeSessionData(final String sessionId, final SessionData data) {
        try {
            redisTemplate.opsForValue().set(sessDataKey(sessionId), objectMapper.writeValueAsString(data), ttl());
        } catch (final JsonProcessingException e) {
            log.error("세션 데이터 직렬화 실패 (sessionId={})", sessionId, e);
        }
    }

    private Duration ttl() {
        return Duration.ofDays(sessionTtlDays);
    }

    private String sessKey(final UUID authServerId, final String deviceId) {
        return SESS_PREFIX + authServerId + ":" + deviceId;
    }

    private String sessDataKey(final String sessionId) {
        return SESSDATA_PREFIX + sessionId;
    }

    public record SessionData(
            UUID userId,
            UUID authServerId,
            String email,
            String deviceId,
            String idToken,
            String role,
            boolean blocked) {
    }
}
