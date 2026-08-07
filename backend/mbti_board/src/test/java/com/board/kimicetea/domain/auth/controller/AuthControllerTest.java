package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.domain.auth.service.AuthService;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.oauth2.RedisOAuth2AuthorizedClientService;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 컨트롤러 슬라이스 테스트 — 로그인/가입/비밀번호 재설정은 AS(Authorization Server)가
 * 호스팅하므로 이 컨트롤러엔 로그아웃만 남는다(connect_manual.md 0번 원칙). 재발급(reissue)은 브라우저가
 * 들고 있는 토큰이 없어져 개념 자체가 사라졌다 — AS access token 갱신은 SessionAuthenticationFilter가
 * 매 요청마다 조용히 처리한다.
 * 주의: @WebMvcTest 슬라이스 — AOP(@RateLimit)는 로드되지 않으므로 Rate Limit 동작은 통합 테스트에서 검증
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // SecurityConfig 의존성 — @WebMvcTest가 Security 설정을 로드하므로 필수
    @MockBean
    private SessionService sessionService;

    @MockBean
    private AuthorizedClientRefreshService authorizedClientRefreshService;

    @MockBean
    private RedisOAuth2AuthorizedClientService redisOAuth2AuthorizedClientService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String LOGOUT_URL     = "/api/auth/logout";
    private static final String LOGOUT_ALL_URL = "/api/auth/logout/all";
    private static final String SESSION_ID     = "valid-session-id";

    private Cookie sessionCookie() {
        return new Cookie(SessionCookieUtils.SESSION_COOKIE_NAME, SESSION_ID);
    }

    @Nested
    @DisplayName("POST /api/auth/logout — 로그아웃")
    class Logout {

        @Test
        @DisplayName("성공 — 세션 쿠키가 있으면 세션을 삭제하고 200을 반환한다")
        void success() throws Exception {
            given(authService.logout(eq(SESSION_ID), any())).willReturn(new LogoutResponse(null));

            mockMvc.perform(post(LOGOUT_URL).cookie(sessionCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — 소셜 로그인 세션이면 authServerLogoutUrl을 응답에 함께 반환한다")
        void returnsAuthServerLogoutUrl() throws Exception {
            given(authService.logout(eq(SESSION_ID), any()))
                    .willReturn(new LogoutResponse("http://localhost:9000/connect/logout?id_token_hint=abc"));

            mockMvc.perform(post(LOGOUT_URL).cookie(sessionCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.authServerLogoutUrl").value("http://localhost:9000/connect/logout?id_token_hint=abc"));
        }

        @Test
        @DisplayName("실패 — 세션 쿠키 없음 → 401")
        void missingSessionCookie() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout/all — 전체 기기 로그아웃")
    class LogoutAll {

        @Test
        @DisplayName("성공 — 세션 쿠키가 있으면 전체 기기 세션을 삭제하고 200을 반환한다")
        void success() throws Exception {
            given(authService.logoutAll(eq(SESSION_ID), any())).willReturn(new LogoutResponse(null));

            mockMvc.perform(post(LOGOUT_ALL_URL).cookie(sessionCookie()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 세션 쿠키 없음 → 401")
        void missingSessionCookie() throws Exception {
            mockMvc.perform(post(LOGOUT_ALL_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ErrorCode.MISSING_REFRESH_TOKEN이 실제로 401로 매핑되는지 방어적으로 재확인(GlobalExceptionHandler 위임)
    @Test
    @DisplayName("컨트롤러가 던진 CustomException(MISSING_REFRESH_TOKEN)은 401 UNAUTHORIZED로 응답한다")
    void missingRefreshTokenMapsToUnauthorized() {
        final CustomException exception = new CustomException(ErrorCode.MISSING_REFRESH_TOKEN);
        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode().getHttpStatus().value()).isEqualTo(401);
    }
}
