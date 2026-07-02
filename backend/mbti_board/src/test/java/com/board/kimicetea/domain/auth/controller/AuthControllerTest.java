package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.EmailVerifyConfirmRequest;
import com.board.kimicetea.domain.auth.dto.EmailVerifySendRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailResponse;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.OAuth2SignupConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetRequestBody;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 컨트롤러 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — AOP(@RateLimit)는 로드되지 않으므로 Rate Limit 동작은 통합 테스트에서 검증
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private static final String FIND_EMAIL_URL           = "/api/auth/find-email";
    private static final String PW_RESET_REQUEST_URL     = "/api/auth/password-reset/request";
    private static final String PW_RESET_CONFIRM_URL     = "/api/auth/password-reset/confirm";
    private static final String EMAIL_VERIFY_SEND_URL    = "/api/auth/email-verify/send";
    private static final String EMAIL_VERIFY_CONFIRM_URL = "/api/auth/email-verify/confirm";
    private static final String SIGNUP_URL               = "/api/auth/signup";
    private static final String OAUTH2_SIGNUP_URL         = "/api/auth/oauth2/signup";
    private static final String LOGIN_URL                = "/api/auth/login";
    private static final String REISSUE_URL              = "/api/auth/reissue";
    private static final String LOGOUT_URL               = "/api/auth/logout";

    private static final String VALID_PHONE    = "010-1234-5678";
    private static final String VALID_EMAIL    = "test@example.com";
    private static final String VALID_PASSWORD = "password123!";
    private static final String VALID_NICKNAME = "테스트유저";
    private static final String VALID_MBTI     = "INTJ";
    private static final String VALID_TOKEN    = "550e8400-e29b-41d4-a716-446655440000";

    private UsernamePasswordAuthenticationToken testAuthentication() {
        final CustomUserDetails details = new CustomUserDetails(
                UUID.randomUUID(), VALID_EMAIL, null, "USER", "device-001");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ────────────────────── POST /api/auth/find-email ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/find-email — 전화번호로 이메일 찾기")
    class FindEmail {

        @Test
        @DisplayName("성공 — 유효한 전화번호 → 200 및 마스킹된 이메일 반환")
        void success() throws Exception {
            given(authService.findEmail(any()))
                    .willReturn(new FindEmailResponse("te***@example.com", false));

            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FindEmailRequest(VALID_PHONE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.maskedEmail").value("te***@example.com"))
                    .andExpect(jsonPath("$.data.isSocialOnly").value(false));
        }

        @Test
        @DisplayName("성공 — 소셜 계정인 경우 isSocialOnly=true 반환")
        void socialOnlyAccount() throws Exception {
            given(authService.findEmail(any()))
                    .willReturn(new FindEmailResponse("te***@example.com", true));

            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FindEmailRequest(VALID_PHONE))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isSocialOnly").value(true));
        }

        @Test
        @DisplayName("실패 — phone 필드 누락(@NotBlank 위반) → 400")
        void missingPhone() throws Exception {
            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 전화번호 형식 오류(@Pattern 위반) → 400")
        void invalidPhoneFormat() throws Exception {
            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone\":\"12345\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 전화번호에 해당하는 사용자 없음 → 404")
        void userNotFound() throws Exception {
            given(authService.findEmail(any()))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FindEmailRequest(VALID_PHONE))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 — 탈퇴 처리 중인 계정 → 403")
        void withdrawnAccount() throws Exception {
            given(authService.findEmail(any()))
                    .willThrow(new CustomException(ErrorCode.ACCOUNT_WITHDRAWN));

            mockMvc.perform(post(FIND_EMAIL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new FindEmailRequest(VALID_PHONE))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────────── POST /api/auth/password-reset/request ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/password-reset/request — 비밀번호 재설정 이메일 발송")
    class RequestPasswordReset {

        @Test
        @DisplayName("성공 — 유효한 이메일 → 200 (이메일 존재 여부와 무관하게 항상 동일 응답)")
        void success() throws Exception {
            willDoNothing().given(authService).requestPasswordReset(any());

            mockMvc.perform(post(PW_RESET_REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PasswordResetRequestBody(VALID_EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 이메일 형식 오류(@Email 위반) → 400")
        void invalidEmailFormat() throws Exception {
            mockMvc.perform(post(PW_RESET_REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — email 필드 누락(@NotBlank 위반) → 400")
        void missingEmail() throws Exception {
            mockMvc.perform(post(PW_RESET_REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────────── POST /api/auth/password-reset/confirm ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/password-reset/confirm — 비밀번호 재설정 확인")
    class ConfirmPasswordReset {

        @Test
        @DisplayName("성공 — 유효한 토큰과 새 비밀번호 → 200")
        void success() throws Exception {
            willDoNothing().given(authService).confirmPasswordReset(any(), any());

            mockMvc.perform(post(PW_RESET_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PasswordResetConfirmRequest(VALID_TOKEN, VALID_PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — token 필드 누락(@NotBlank 위반) → 400")
        void missingToken() throws Exception {
            mockMvc.perform(post(PW_RESET_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"" + VALID_PASSWORD + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 비밀번호 10자 미만(@Size 위반) → 400")
        void passwordTooShort() throws Exception {
            mockMvc.perform(post(PW_RESET_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"" + VALID_TOKEN + "\",\"newPassword\":\"short1\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 비밀번호 영문+숫자 미포함(@Pattern 위반) → 400")
        void invalidPasswordPattern() throws Exception {
            mockMvc.perform(post(PW_RESET_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\":\"" + VALID_TOKEN + "\",\"newPassword\":\"onlychars!!!\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 만료되거나 유효하지 않은 재설정 토큰 → 400")
        void invalidResetToken() throws Exception {
            willThrow(new CustomException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID))
                    .given(authService).confirmPasswordReset(any(), any());

            mockMvc.perform(post(PW_RESET_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PasswordResetConfirmRequest("expired-token", VALID_PASSWORD))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PASSWORD_RESET_TOKEN_INVALID.getMessage()));
        }
    }

    // ────────────────────── POST /api/auth/email-verify/send ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/email-verify/send — 이메일 인증 발송")
    class SendEmailVerification {

        @Test
        @DisplayName("성공 — 미가입 이메일 → 200")
        void success() throws Exception {
            willDoNothing().given(authService).sendEmailVerification(any());

            mockMvc.perform(post(EMAIL_VERIFY_SEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmailVerifySendRequest(VALID_EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 이메일 형식 오류(@Email 위반) → 400")
        void invalidEmailFormat() throws Exception {
            mockMvc.perform(post(EMAIL_VERIFY_SEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"invalid\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — email 필드 누락(@NotBlank 위반) → 400")
        void missingEmail() throws Exception {
            mockMvc.perform(post(EMAIL_VERIFY_SEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 이미 가입된 이메일 → 409")
        void duplicateEmail() throws Exception {
            willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL))
                    .given(authService).sendEmailVerification(any());

            mockMvc.perform(post(EMAIL_VERIFY_SEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmailVerifySendRequest(VALID_EMAIL))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_EMAIL.getMessage()));
        }
    }

    // ────────────────────── POST /api/auth/email-verify/confirm ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/email-verify/confirm — 이메일 인증 확인")
    class ConfirmEmailVerification {

        @Test
        @DisplayName("성공 — 유효한 인증 토큰 → 200 및 인증 완료 이메일 반환")
        void success() throws Exception {
            given(authService.confirmEmailVerification(any())).willReturn(VALID_EMAIL);

            mockMvc.perform(post(EMAIL_VERIFY_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmailVerifyConfirmRequest(VALID_TOKEN))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(VALID_EMAIL));
        }

        @Test
        @DisplayName("실패 — token 필드 누락(@NotBlank 위반) → 400")
        void missingToken() throws Exception {
            mockMvc.perform(post(EMAIL_VERIFY_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 만료되거나 유효하지 않은 인증 토큰 → 400")
        void invalidToken() throws Exception {
            given(authService.confirmEmailVerification(any()))
                    .willThrow(new CustomException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID));

            mockMvc.perform(post(EMAIL_VERIFY_CONFIRM_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new EmailVerifyConfirmRequest("expired-token"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID.getMessage()));
        }
    }

    // ────────────────────── POST /api/auth/signup ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/signup — 회원가입")
    class Signup {

        private String signupBody(final String email, final String password,
                final String nickname, final String mbti, final String phone) throws Exception {
            return objectMapper.writeValueAsString(new SignupRequest(email, password, nickname, mbti, phone, true, true));
        }

        @Test
        @DisplayName("성공 — 유효한 요청 → 201")
        void success() throws Exception {
            willDoNothing().given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, VALID_PHONE)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — phone 미입력(null) 허용 → 201")
        void successWithoutPhone() throws Exception {
            willDoNothing().given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — mbtiId에 NONE 전달 허용 → 201")
        void successWithMbtiNone() throws Exception {
            willDoNothing().given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, "NONE", null)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 이메일 형식 오류(@Email 위반) → 400")
        void invalidEmail() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody("not-an-email", VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 비밀번호 영문+숫자 미포함(@Pattern 위반) → 400")
        void invalidPasswordPattern() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, "onlychars!!!", VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 비밀번호 10자 미만(@Size 위반) → 400")
        void passwordTooShort() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, "pw1", VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 닉네임 2자 미만(@Size 위반) → 400")
        void nicknameTooShort() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, "a", VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 닉네임 12자 초과(@Size 위반) → 400")
        void nicknameTooLong() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, "닉네임이열세자이상이면안됩니다", VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 닉네임 특수문자 포함(@Pattern 위반) → 400")
        void invalidNicknamePattern() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, "nick@name", VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 잘못된 MBTI 값(@Pattern 위반) → 400")
        void invalidMbti() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, "XXXX", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 전화번호 형식 오류(@Pattern 위반) → 400")
        void invalidPhoneFormat() throws Exception {
            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, "12345")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 약관 미동의 → 400")
        void termsNotAgreed() throws Exception {
            willThrow(new CustomException(ErrorCode.TERMS_NOT_AGREED))
                    .given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SignupRequest(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null, false, true))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.TERMS_NOT_AGREED.getMessage()));
        }

        @Test
        @DisplayName("실패 — 이메일 인증 미완료 → 400")
        void emailNotVerified() throws Exception {
            willThrow(new CustomException(ErrorCode.EMAIL_NOT_VERIFIED))
                    .given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_NOT_VERIFIED.getMessage()));
        }

        @Test
        @DisplayName("실패 — 이메일 중복 → 409")
        void duplicateEmail() throws Exception {
            willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL))
                    .given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 닉네임 중복 → 409")
        void duplicateNickname() throws Exception {
            willThrow(new CustomException(ErrorCode.DUPLICATE_NICKNAME))
                    .given(authService).signup(any(), any());

            mockMvc.perform(post(SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(signupBody(VALID_EMAIL, VALID_PASSWORD, VALID_NICKNAME, VALID_MBTI, null)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────────── POST /api/auth/login ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/login — 로컬 로그인")
    class Login {

        @Test
        @DisplayName("성공 — 유효한 이메일·비밀번호 → 200 및 AT 반환")
        void success() throws Exception {
            given(authService.login(any(), any(), any()))
                    .willReturn(new LoginResponse("access-token", VALID_EMAIL, VALID_NICKNAME, "USER"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(VALID_EMAIL, VALID_PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.data.nickname").value(VALID_NICKNAME))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("실패 — 이메일 형식 오류(@Email 위반) → 400")
        void invalidEmail() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"bad\",\"password\":\"" + VALID_PASSWORD + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — password 필드 누락(@NotBlank 위반) → 400")
        void missingPassword() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + VALID_EMAIL + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 이메일/비밀번호 불일치 → 401")
        void invalidCredentials() throws Exception {
            given(authService.login(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.INVALID_CREDENTIALS));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(VALID_EMAIL, VALID_PASSWORD))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
        }

        @Test
        @DisplayName("실패 — 소셜 전용 계정 → 400")
        void socialOnlyAccount() throws Exception {
            given(authService.login(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.SOCIAL_ONLY_ACCOUNT));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(VALID_EMAIL, VALID_PASSWORD))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 잠긴 계정 → 403")
        void lockedAccount() throws Exception {
            given(authService.login(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.ACCOUNT_LOCKED));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(VALID_EMAIL, VALID_PASSWORD))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 영구 정지 계정 → 403")
        void bannedAccount() throws Exception {
            given(authService.login(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.ACCOUNT_BANNED));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(VALID_EMAIL, VALID_PASSWORD))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────────── POST /api/auth/oauth2/signup ──────────────────────

    @Nested
    @DisplayName("POST /api/auth/oauth2/signup — 소셜 가입 약관 동의 완료")
    class ConfirmOAuth2Signup {

        private static final String VALID_SIGNUP_TOKEN = "valid-signup-token";

        private String body(final String signupToken, final boolean termsAgreed, final boolean privacyAgreed)
                throws Exception {
            return objectMapper.writeValueAsString(
                    new OAuth2SignupConfirmRequest(signupToken, termsAgreed, privacyAgreed));
        }

        @Test
        @DisplayName("성공 — 유효한 signupToken과 약관 동의 → 200 및 AT 반환")
        void success() throws Exception {
            given(authService.confirmOAuth2Signup(any(), any(), any()))
                    .willReturn(new LoginResponse("access-token", VALID_EMAIL, VALID_NICKNAME, "USER"));

            mockMvc.perform(post(OAUTH2_SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(VALID_SIGNUP_TOKEN, true, true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.data.nickname").value(VALID_NICKNAME))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("실패 — signupToken 필드 누락(@NotBlank 위반) → 400")
        void missingSignupToken() throws Exception {
            mockMvc.perform(post(OAUTH2_SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"termsAgreed\":true,\"privacyAgreed\":true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 약관 미동의 → 400")
        void termsNotAgreed() throws Exception {
            given(authService.confirmOAuth2Signup(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.TERMS_NOT_AGREED));

            mockMvc.perform(post(OAUTH2_SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(VALID_SIGNUP_TOKEN, false, true)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.TERMS_NOT_AGREED.getMessage()));
        }

        @Test
        @DisplayName("실패 — 만료되거나 유효하지 않은 signupToken → 400")
        void invalidSignupToken() throws Exception {
            given(authService.confirmOAuth2Signup(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.OAUTH2_SIGNUP_TOKEN_INVALID));

            mockMvc.perform(post(OAUTH2_SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("expired-token", true, true)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.OAUTH2_SIGNUP_TOKEN_INVALID.getMessage()));
        }

        @Test
        @DisplayName("실패 — 경합 상태로 인한 이메일 중복 → 409")
        void duplicateEmail() throws Exception {
            given(authService.confirmOAuth2Signup(any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

            mockMvc.perform(post(OAUTH2_SIGNUP_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(VALID_SIGNUP_TOKEN, true, true)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
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
