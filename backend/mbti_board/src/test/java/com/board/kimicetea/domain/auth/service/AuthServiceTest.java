package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SESSION_ID = "session-1";
    private static final UUID AUTH_SERVER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    /** SessionData.idToken은 로그아웃 URL 조립에 더 이상 쓰이지 않는다 — 자리만 채우는 더미 */
    private static final String ID_TOKEN = "unused-id-token";
    private static final String FRONTEND_URL = "http://localhost:4000";

    @Mock
    private SessionService sessionService;

    @Mock
    private SessionCookieUtils sessionCookieUtils;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private AuthorizedClientRefreshService authorizedClientRefreshService;

    @InjectMocks
    private AuthService authService;

    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", FRONTEND_URL);
        response = mock(HttpServletResponse.class);
    }

    private SessionService.SessionData sessionData(final String idToken) {
        return new SessionService.SessionData(
                USER_ID, AUTH_SERVER_ID, "user@example.com", "device-001", idToken, "USER", false);
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("thebluebutter")
                .clientId("TypeButter")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/thebluebutter")
                .authorizationUri("http://localhost:9000/oauth2/authorize")
                .tokenUri("http://localhost:9000/oauth2/token")
                .issuerUri("http://localhost:9000")
                .scope("openid", "profile")
                .build();
    }

    @Nested
    @DisplayName("logout — 단일 기기 로그아웃")
    class Logout {

        @Test
        @DisplayName("세션이 있으면 삭제하고, 쿠키를 지우고, client_id 기반 AS 로그아웃 URL을 조립해 반환한다")
        void deletesSessionAndBuildsAuthServerLogoutUrl() {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData(ID_TOKEN)));
            when(clientRegistrationRepository.findByRegistrationId("thebluebutter")).thenReturn(clientRegistration());

            final LogoutResponse result = authService.logout(SESSION_ID, response);

            verify(authorizedClientRefreshService).revokeForLogout(AUTH_SERVER_ID, "device-001");
            verify(sessionService).deleteSession(SESSION_ID);
            verify(sessionCookieUtils).clearSessionCookie(response);
            assertThat(result.authServerLogoutUrl())
                    .startsWith("http://localhost:9000/session/logout")
                    .contains("client_id=TypeButter")
                    .contains("post_logout_redirect_uri=" + FRONTEND_URL)
                    .doesNotContain("id_token_hint")
                    .doesNotContain("/connect/logout");
        }

        @Test
        @DisplayName("이미 사라진 세션이어도 쿠키는 지우고 authServerLogoutUrl 없이 정상 응답한다")
        void missingSessionStillClearsCookie() {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.empty());

            final LogoutResponse result = authService.logout(SESSION_ID, response);

            verifyNoInteractions(authorizedClientRefreshService);
            verify(sessionService).deleteSession(SESSION_ID);
            verify(sessionCookieUtils).clearSessionCookie(response);
            assertThat(result.authServerLogoutUrl()).isNull();
            verifyNoInteractions(clientRegistrationRepository);
        }
    }

    @Nested
    @DisplayName("logoutAll — 전체 기기 로그아웃")
    class LogoutAll {

        @Test
        @DisplayName("세션이 있으면 해당 authServerId의 모든 세션을 삭제하고 client_id 기반 AS 로그아웃 URL을 반환한다")
        void deletesAllSessionsForAuthServerId() {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.of(sessionData(ID_TOKEN)));
            when(clientRegistrationRepository.findByRegistrationId("thebluebutter")).thenReturn(clientRegistration());

            final LogoutResponse result = authService.logoutAll(SESSION_ID, response);

            verify(sessionService).deleteAllSessions(AUTH_SERVER_ID);
            verify(sessionService, never()).deleteSession(anyString());
            verify(sessionCookieUtils).clearSessionCookie(response);
            assertThat(result.authServerLogoutUrl())
                    .startsWith("http://localhost:9000/session/logout")
                    .contains("client_id=TypeButter");
        }

        @Test
        @DisplayName("이미 사라진 세션이면 deleteAllSessions를 호출하지 않고도 쿠키는 지운다")
        void missingSessionSkipsDeleteAllSessions() {
            when(sessionService.getSessionData(SESSION_ID)).thenReturn(Optional.empty());

            final LogoutResponse result = authService.logoutAll(SESSION_ID, response);

            verify(sessionService, never()).deleteAllSessions(any());
            verify(sessionCookieUtils).clearSessionCookie(response);
            assertThat(result.authServerLogoutUrl()).isNull();
        }
    }
}
