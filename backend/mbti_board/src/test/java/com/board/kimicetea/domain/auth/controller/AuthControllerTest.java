package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.service.AuthService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 컨트롤러 슬라이스 테스트 — 로그인/가입/비밀번호 재설정은 AS(Authorization Server)가
 * 호스팅하므로 이 컨트롤러엔 재발급·로그아웃만 남는다(connect_manual.md 0번 원칙).
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
    private JwtProvider jwtProvider;

    @MockBean
    private TokenRedisService tokenRedisService;

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

    private static final String REISSUE_URL = "/api/auth/reissue";
    private static final String LOGOUT_URL  = "/api/auth/logout";

    private static final String VALID_EMAIL    = "test@example.com";
    private static final String VALID_NICKNAME = "테스트유저";

    private UsernamePasswordAuthenticationToken testAuthentication() {
        final CustomUserDetails details = new CustomUserDetails(
                UUID.randomUUID(), VALID_EMAIL, null, "USER", "device-001");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ────────────────────── POST /api/auth/reissue ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/reissue — 토큰 재발급 (RT Rotation)")
    class Reissue {

        @Test
        @DisplayName("성공 — 유효한 RT Cookie → 200 및 새 AT 반환")
        void success() throws Exception {
            given(authService.reissue(any(), any()))
                    .willReturn(new LoginResponse("new-access-token", VALID_EMAIL, VALID_NICKNAME, "USER"));

            mockMvc.perform(post(REISSUE_URL)
                            .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("실패 — RT 쿠키 없음 → 401")
        void missingRefreshTokenCookie() throws Exception {
            given(authService.reissue(any(), any()))
                    .willThrow(new CustomException(ErrorCode.MISSING_REFRESH_TOKEN));

            mockMvc.perform(post(REISSUE_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — RT 만료 → 401")
        void expiredRefreshToken() throws Exception {
            given(authService.reissue(any(), any()))
                    .willThrow(new CustomException(ErrorCode.EXPIRED_TOKEN));

            mockMvc.perform(post(REISSUE_URL)
                            .cookie(new Cookie("refresh_token", "expired-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — RT 재사용 감지 → 401 (전 기기 로그아웃 처리)")
        void reuseDetected() throws Exception {
            given(authService.reissue(any(), any()))
                    .willThrow(new CustomException(ErrorCode.REUSE_DETECTED));

            mockMvc.perform(post(REISSUE_URL)
                            .cookie(new Cookie("refresh_token", "reused-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.REUSE_DETECTED.getMessage()));
        }

        @Test
        @DisplayName("실패 — 유효하지 않은 RT → 401")
        void invalidToken() throws Exception {
            given(authService.reissue(any(), any()))
                    .willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

            mockMvc.perform(post(REISSUE_URL)
                            .cookie(new Cookie("refresh_token", "tampered-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────────── POST /api/auth/logout ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/logout — 로그아웃")
    class Logout {

        @BeforeEach
        void setAuthentication() {
            SecurityContextHolder.getContext().setAuthentication(testAuthentication());
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("성공 — Authorization 헤더와 인증 정보 → 200")
        void success() throws Exception {
            willDoNothing().given(authService).logout(any(), any(), any(), any());

            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer valid-access-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — Authorization 헤더 누락 → 400")
        void missingAuthorizationHeader() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isBadRequest());
        }
    }
}
