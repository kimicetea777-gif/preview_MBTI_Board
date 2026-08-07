package com.board.kimicetea.global.security.session;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshUnavailableException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    private static final String SESSION_ID = "session-1";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID AUTH_SERVER_ID = UUID.randomUUID();
    private static final String DEVICE_ID = "device-001";

    @Mock
    private SessionService sessionService;

    @Mock
    private AuthorizedClientRefreshService authorizedClientRefreshService;

    @Mock
    private UserRepository userRepository;

    private SessionAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new SessionAuthenticationFilter(sessionService, authorizedClientRefreshService, userRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SessionService.SessionData sessionData(final String role, final boolean blocked) {
        return new SessionService.SessionData(
                USER_ID, AUTH_SERVER_ID, "user@example.com", DEVICE_ID, "id-token", role, blocked);
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

    @Test
    @DisplayName("세션 쿠키가 없으면 아무 것도 하지 않고 다음 필터로 넘어간다")
    void noCookiePassesThrough() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isNull();
        verifyNoInteractions(sessionService, authorizedClientRefreshService, userRepository);
    }

    @Test
    @DisplayName("세션 쿠키는 있지만 세션 데이터가 없으면 SESSION_INVALID를 남기고 통과시킨다")
    void missingSessionDataSetsError() throws Exception {
        request.setCookies(new Cookie(SessionCookieUtils.SESSION_COOKIE_NAME, SESSION_ID));
        when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("SESSION_INVALID");
        verifyNoInteractions(authorizedClientRefreshService);
    }

    @Test
    @DisplayName("세션 조회 자체가 Redis 장애로 실패해도 세션을 지우지 않고 이번 요청만 미인증 처리한다")
    void sessionLookupRedisFailureDoesNotDeleteSession() throws Exception {
        request.setCookies(new Cookie(SessionCookieUtils.SESSION_COOKIE_NAME, SESSION_ID));
        when(sessionService.getSessionData(SESSION_ID)).thenThrow(new RedisConnectionFailureException("boom"));

        filter.doFilter(request, response, filterChain);

        verify(sessionService, never()).deleteSession(anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("TEMPORARILY_UNAVAILABLE");
        verifyNoInteractions(authorizedClientRefreshService);
    }

    @Nested
    @DisplayName("유효한 세션")
    class ValidSession {

        @BeforeEach
        void stubCookie() {
            request.setCookies(new Cookie(SessionCookieUtils.SESSION_COOKIE_NAME, SESSION_ID));
        }

        @Test
        @DisplayName("refresh가 필요 없으면 캐시된 role/blocked 그대로 인증을 등록한다")
        void unchangedRegistersAuthenticationFromCache() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenReturn(new AuthorizedClientRefreshService.RefreshOutcome(null, false, false));

            filter.doFilter(request, response, filterChain);

            final CustomUserDetails principal =
                    (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.userId()).isEqualTo(USER_ID);
            assertThat(principal.role()).isEqualTo("USER");
            assertThat(principal.deviceId()).isEqualTo(DEVICE_ID);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refresh가 일어났으면 로컬 User를 재조회해 role/blocked 캐시를 갱신하고 그 값으로 인증한다")
        void refreshedUpdatesCacheFromLocalUser() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenReturn(new AuthorizedClientRefreshService.RefreshOutcome(null, true, false));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserRole.ADMIN, UserState.NORMAL)));

            filter.doFilter(request, response, filterChain);

            verify(sessionService).updateRoleAndBlocked(eq(SESSION_ID), any(), eq("ADMIN"), eq(false));
            final CustomUserDetails principal =
                    (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.role()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("refresh 중 로컬 User를 찾지 못하면 기존 캐시값을 그대로 쓰고 캐시 갱신은 하지 않는다")
        void refreshedButUserMissingKeepsOldCache() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenReturn(new AuthorizedClientRefreshService.RefreshOutcome(null, true, false));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            filter.doFilter(request, response, filterChain);

            verify(sessionService, never()).updateRoleAndBlocked(any(), any(), any(), anyBoolean());
            final CustomUserDetails principal =
                    (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("refresh 후 차단 상태로 확인되면 세션을 지우고 인증하지 않는다")
        void refreshedAndBlockedDeletesSession() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenReturn(new AuthorizedClientRefreshService.RefreshOutcome(null, true, false));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserRole.USER, UserState.WITHDRAWN)));

            filter.doFilter(request, response, filterChain);

            verify(sessionService).deleteSession(SESSION_ID);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("ACCOUNT_BLOCKED");
        }

        @Test
        @DisplayName("AS가 명시적으로 거부(rejected)하면 세션을 지우고 SESSION_INVALID로 처리한다")
        void rejectedDeletesSession() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenReturn(new AuthorizedClientRefreshService.RefreshOutcome(null, false, true));

            filter.doFilter(request, response, filterChain);

            verify(sessionService).deleteSession(SESSION_ID);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("SESSION_INVALID");
        }

        @Test
        @DisplayName("Redis 장애 시 세션은 지우지 않고 이번 요청만 미인증 처리한다")
        void redisFailureDoesNotDeleteSession() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenThrow(new RedisConnectionFailureException("boom"));

            filter.doFilter(request, response, filterChain);

            verify(sessionService, never()).deleteSession(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("TEMPORARILY_UNAVAILABLE");
        }

        @Test
        @DisplayName("AS 통신 장애 시에도 세션은 지우지 않고 이번 요청만 미인증 처리한다")
        void asUnavailableDoesNotDeleteSession() throws Exception {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData("USER", false)));
            when(authorizedClientRefreshService.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .thenThrow(new AuthorizedClientRefreshUnavailableException("AS 서버 통신 실패", new RuntimeException()));

            filter.doFilter(request, response, filterChain);

            verify(sessionService, never()).deleteSession(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE)).isEqualTo("TEMPORARILY_UNAVAILABLE");
        }
    }
}
