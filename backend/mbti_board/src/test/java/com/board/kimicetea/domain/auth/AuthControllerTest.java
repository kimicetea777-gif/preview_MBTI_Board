package com.board.kimicetea.domain.auth;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.EmailVerifyConfirmRequest;
import com.board.kimicetea.domain.auth.dto.EmailVerifySendRequest;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증 통합 테스트 (1~4단계, 7~9단계)
 * 주의: 각 테스트는 독립적으로 실행 가능해야 하며, @AfterEach 에서 테스트 유저를 정리합니다.
 * 주의: EmailService는 @MockBean — 실제 이메일 발송 없이 Redis에 직접 마킹해 이메일 인증 우회
 */
@AutoConfigureMockMvc
class AuthControllerTest extends IntegrationTestSupport {

        @Autowired MockMvc mockMvc;
        @Autowired ObjectMapper objectMapper;
        @Autowired UserRepository userRepository;
        @Autowired MbtiMetadataRepository mbtiMetadataRepository;
        @Autowired TokenRedisService tokenRedisService;
        @Autowired JwtProvider jwtProvider;
        @Autowired StringRedisTemplate redisTemplate;
        @MockBean  EmailService emailService;

        private static final String SIGNUP_URL              = "/api/auth/signup";
        private static final String LOGIN_URL               = "/api/auth/login";
        private static final String LOGOUT_URL              = "/api/auth/logout";
        private static final String REISSUE_URL             = "/api/auth/reissue";
        private static final String EMAIL_VERIFY_SEND_URL   = "/api/auth/email-verify/send";
        private static final String EMAIL_VERIFY_CONFIRM_URL = "/api/auth/email-verify/confirm";

        private static final String SOCIAL_EMAIL  = "social-auth-test@example.com";
        private static final String TEST_EMAIL    = "auth-test@example.com";
        private static final String TEST_PASSWORD = "password123!";
        private static final String TEST_NICKNAME = "테스트유저";
        private static final String TEST_MBTI     = "INTJ";

        @AfterEach
        void tearDown() {
                userRepository.deleteAll();
                redisTemplate.execute((RedisCallback<Void>) connection -> {
                        connection.serverCommands().flushAll();
                        return null;
                });
        }

        // ─────────────────────────────────────────────────────────────
        // 1-1. 회원가입 성공
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-1. 회원가입 성공 → 201 Created")
        void signup_성공() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);

                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 1-2. 중복 이메일 회원가입 → 409 Conflict
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-2. 중복 이메일 회원가입 → 409 Conflict")
        void signup_중복_이메일_실패() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, "다른닉네임", TEST_MBTI))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 1-3. 중복 닉네임 회원가입 → 409 Conflict
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-3. 중복 닉네임 회원가입 → 409 Conflict")
        void signup_중복_닉네임_실패() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                tokenRedisService.markEmailVerified("other@example.com");
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest("other@example.com", TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 1-4. 로그인 성공 → 200 OK + accessToken + Set-Cookie
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-4. 로그인 성공 → 200 OK, accessToken body, refreshToken Cookie")
        void login_성공() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                                .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME))
                                .andExpect(jsonPath("$.data.role").isNotEmpty())
                                .andExpect(cookie().exists("refreshToken"))
                                .andExpect(cookie().httpOnly("refreshToken", true));
        }

        // ─────────────────────────────────────────────────────────────
        // 1-5. 잘못된 비밀번호 로그인 → 401 Unauthorized
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-5. 잘못된 비밀번호 로그인 → 401 Unauthorized")
        void login_잘못된_비밀번호_실패() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, "wrongpassword"))))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 2단계 — 인증 필터 검증
        // ─────────────────────────────────────────────────────────────

        /** 회원가입 → 로그인을 수행하고 AT를 추출하는 헬퍼 */
        private String 회원가입_후_로그인_AT_추출() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("accessToken").asText();
        }

        // ─────────────────────────────────────────────────────────────
        // 2-1. AT 없이 인증 필요 엔드포인트 요청 → 401
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("2-1. AT 없이 인증 필요 엔드포인트 요청 → 401, '인증이 필요합니다.'")
        void 인증필터_AT_없음_실패() throws Exception {
                mockMvc.perform(post(LOGOUT_URL))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 2-2. 유효한 AT로 요청 → 200 OK
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("2-2. 유효한 AT로 인증 필요 엔드포인트 요청 → 200 OK")
        void 인증필터_유효한_AT_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        // ─────────────────────────────────────────────────────────────
        // 2-3. 변조된 AT 요청 → 401, '토큰이 만료되었습니다.'
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("2-3. 변조된 AT로 요청 → 401, '토큰이 만료되었습니다.'")
        void 인증필터_변조된_AT_실패() throws Exception {
                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer invalidtoken"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 3-1. 로그아웃 → 200 OK + BL 키 생성 + RT 키 삭제
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("3-1. 로그아웃 → 200 OK, AT 블랙리스트 등록, RT 삭제")
        void 로그아웃_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                final Claims claims = jwtProvider.parseToken(accessToken);
                final String userId   = claims.getSubject();
                final String deviceId = claims.get("deviceId", String.class);

                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                assertThat(tokenRedisService.isBlacklisted(accessToken)).isTrue();
                assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isNull();
        }

        // ─────────────────────────────────────────────────────────────
        // 3-2. 블랙리스트 AT 재사용 → 401
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("3-2. 로그아웃된 AT 재사용 → 401, '토큰이 만료되었습니다.'")
        void 블랙리스트_AT_재사용_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());

                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 4단계 — 토큰 재발급 (RT Rotation)
        // ─────────────────────────────────────────────────────────────

        private record LoginTokens(String accessToken, String refreshToken) {}

        private LoginTokens 회원가입_후_로그인_토큰_추출() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                final MvcResult result = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn();

                final String at = objectMapper.readTree(result.getResponse().getContentAsString())
                                .path("data").path("accessToken").asText();
                final String rt = setCookie에서_RT값_추출(result.getResponse());
                return new LoginTokens(at, rt);
        }

        private String setCookie에서_RT값_추출(final MockHttpServletResponse response) {
                final String prefix = CookieUtils.REFRESH_TOKEN_COOKIE_NAME + "=";
                return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                                .filter(h -> h.startsWith(prefix))
                                .map(h -> h.split(";")[0].substring(prefix.length()))
                                .findFirst()
                                .orElseThrow();
        }

        // ─────────────────────────────────────────────────────────────
        // 4-1. 정상 재발급 → 200 OK + 새 AT + 새 RT (RT Rotation)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("4-1. RT Cookie로 재발급 → 200 OK, 새 AT·RT 발급, Redis RT 교체")
        void 토큰_재발급_성공() throws Exception {
                final LoginTokens tokens = 회원가입_후_로그인_토큰_추출();
                final Claims claims = jwtProvider.parseToken(tokens.accessToken());
                final String userId   = claims.getSubject();
                final String deviceId = claims.get("deviceId", String.class);

                final MvcResult result = mockMvc.perform(post(REISSUE_URL)
                                .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                                .andReturn();

                final String newRt = setCookie에서_RT값_추출(result.getResponse());
                assertThat(newRt).isNotEqualTo(tokens.refreshToken());
                assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isEqualTo(newRt);
        }

        // ─────────────────────────────────────────────────────────────
        // 4-2. RT Cookie 없이 재발급 요청 → 401
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("4-2. RT Cookie 없이 재발급 요청 → 401, 'Refresh Token 쿠키가 없습니다.'")
        void 재발급_RT_쿠키_없음_실패() throws Exception {
                mockMvc.perform(post(REISSUE_URL))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Refresh Token 쿠키가 없습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 4-3. RT Reuse Detection → 401 + 전체 기기 RT 즉시 삭제
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("4-3. 구 RT 재사용 감지 → 401, 해당 유저 모든 RT 즉시 삭제")
        void RT_재사용_감지_실패() throws Exception {
                final LoginTokens tokens = 회원가입_후_로그인_토큰_추출();
                final Claims claims = jwtProvider.parseToken(tokens.accessToken());
                final String userId   = claims.getSubject();
                final String deviceId = claims.get("deviceId", String.class);

                mockMvc.perform(post(REISSUE_URL)
                                .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                                .andExpect(status().isOk());

                mockMvc.perform(post(REISSUE_URL)
                                .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다."));

                assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isNull();
        }

        // ─────────────────────────────────────────────────────────────
        // 7-1. 소셜 계정으로 로컬 로그인 시도 → 400
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("7-1. 소셜 계정으로 로컬 로그인 시도 → 400, '소셜 로그인 전용 계정입니다.'")
        void 소셜_계정_로컬_로그인_실패() throws Exception {
                final User socialUser = User.builder()
                                .email(SOCIAL_EMAIL)
                                .nickname("소셜테스트유저")
                                .socialProvider("google")
                                .providerId("test-provider-id-001")
                                .mbti(mbtiMetadataRepository.findById("NONE").orElseThrow())
                                .mbtiVerified(MbtiVerified.DEFAULT)
                                .role(UserRole.USER)
                                .state(UserState.NORMAL)
                                .build();
                userRepository.save(socialUser);

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(SOCIAL_EMAIL, "anypassword"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("소셜 로그인 전용 계정입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 7-2. 계정 상태별 로그인 차단 → 403
        // ─────────────────────────────────────────────────────────────

        static Stream<Arguments> 계정상태_메시지_매핑() {
                return Stream.of(
                                Arguments.of(UserState.LOCKED, "계정이 잠겨 있습니다."),
                                Arguments.of(UserState.WITHDRAWN, "탈퇴 처리 중인 계정입니다."),
                                Arguments.of(UserState.PERMANENT_BAN, "정지된 계정입니다."));
        }

        @ParameterizedTest(name = "7-2. {0} 상태 계정 로그인 → 403, {1}")
        @MethodSource("계정상태_메시지_매핑")
        void 계정상태별_로그인_차단(final UserState state, final String expectedMessage) throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                final User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
                user.changeState(state);
                userRepository.save(user);

                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value(expectedMessage));
        }

        // ─────────────────────────────────────────────────────────────
        // 7-3. 유효하지 않은 MBTI ID로 회원가입 → 404
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("7-3. 유효하지 않은 MBTI ID로 회원가입 → 404, '존재하지 않는 MBTI입니다.'")
        void 유효하지_않은_MBTI_회원가입_실패() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);

                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, "INVALID"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("존재하지 않는 MBTI입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 7-4. 다중 기기 로그인 → 기기A 로그아웃 시 기기B RT 유지
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("7-4. 다중 기기 로그인 → 기기A 로그아웃 시 기기B RT 유지")
        void 다중_기기_로그인_독립_로그아웃() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String atA = 로그인_후_AT_추출();
                final String atB = 로그인_후_AT_추출();
                final Claims claimsA = jwtProvider.parseToken(atA);
                final Claims claimsB = jwtProvider.parseToken(atB);
                final String userId   = claimsA.getSubject();
                final String deviceIdA = claimsA.get("deviceId", String.class);
                final String deviceIdB = claimsB.get("deviceId", String.class);

                mockMvc.perform(post(LOGOUT_URL)
                                .header("Authorization", "Bearer " + atA))
                                .andExpect(status().isOk());

                assertThat(tokenRedisService.getRefreshToken(userId, deviceIdA)).isNull();
                assertThat(tokenRedisService.getRefreshToken(userId, deviceIdB)).isNotNull();
        }

        private String 로그인_후_AT_추출() throws Exception {
                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();
                return objectMapper.readTree(body).path("data").path("accessToken").asText();
        }

        // ─────────────────────────────────────────────────────────────
        // 9단계 — 이메일 인증 플로우
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-1. 이메일 인증 발송 → 200 OK, Redis EV:TOKEN:* 키 생성")
        void 이메일_인증_발송_성공() throws Exception {
                mockMvc.perform(post(EMAIL_VERIFY_SEND_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new EmailVerifySendRequest(TEST_EMAIL))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("인증 이메일이 발송되었습니다."));

                final Set<String> keys = redisTemplate.keys("EV:TOKEN:*");
                assertThat(keys).isNotEmpty();
                assertThat(keys.iterator().next()).startsWith("EV:TOKEN:");
        }

        @Test
        @DisplayName("9-2. 유효한 토큰으로 이메일 인증 확인 → 200 OK, Redis EV:DONE:* 키 생성")
        void 이메일_인증_확인_성공() throws Exception {
                // 토큰을 Redis에 직접 저장해 send 단계 우회
                final String token = "test-verify-token-uuid";
                tokenRedisService.saveEmailVerifyToken(token, TEST_EMAIL);

                mockMvc.perform(post(EMAIL_VERIFY_CONFIRM_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new EmailVerifyConfirmRequest(token))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

                assertThat(tokenRedisService.isEmailVerified(TEST_EMAIL)).isTrue();
                // 토큰은 1회 사용 후 삭제
                assertThat(tokenRedisService.getEmailByVerifyToken(token)).isNull();
        }

        @Test
        @DisplayName("9-3. 유효하지 않은 토큰으로 인증 시도 → 400, '유효하지 않은 인증 링크입니다.'")
        void 이메일_인증_확인_유효하지_않은_토큰_실패() throws Exception {
                mockMvc.perform(post(EMAIL_VERIFY_CONFIRM_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new EmailVerifyConfirmRequest("invalid-token"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("유효하지 않은 인증 링크입니다. 다시 요청해 주세요."));
        }

        @Test
        @DisplayName("9-4. 이메일 인증 없이 회원가입 → 400, '이메일 인증이 완료되지 않았습니다.'")
        void 이메일_인증_없이_회원가입_실패() throws Exception {
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다."));
        }

        @Test
        @DisplayName("9-5. 이메일 인증 완료 후 회원가입 → 201 Created, EV:DONE 키 삭제")
        void 이메일_인증_후_회원가입_성공() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);

                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());

                // 가입 완료 후 EV:DONE 키 제거 확인
                assertThat(tokenRedisService.isEmailVerified(TEST_EMAIL)).isFalse();
        }
}
