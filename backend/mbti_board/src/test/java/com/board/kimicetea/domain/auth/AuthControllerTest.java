package com.board.kimicetea.domain.auth;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증 통합 테스트 (1~4단계, 7단계)
 * 주의: 각 테스트는 독립적으로 실행 가능해야 하며, @AfterEach 에서 테스트 유저를 정리합니다.
 */
@AutoConfigureMockMvc
class AuthControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired MbtiMetadataRepository mbtiMetadataRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired JwtProvider jwtProvider;

    private static final String SIGNUP_URL  = "/api/auth/signup";
    private static final String LOGIN_URL   = "/api/auth/login";
    private static final String LOGOUT_URL  = "/api/auth/logout";
    private static final String REISSUE_URL = "/api/auth/reissue";

    private static final String SOCIAL_EMAIL = "social-auth-test@example.com";

    private static final String TEST_EMAIL    = "auth-test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "테스트유저";
    private static final String TEST_MBTI     = "INTJ";

    @AfterEach
    void tearDown() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(SOCIAL_EMAIL).ifPresent(userRepository::delete);
    }

    // ─────────────────────────────────────────────────────────────
    // 1-1. 회원가입 성공
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-1. 회원가입 성공 → 201 Created")
    void signup_성공() throws Exception {
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
        // given — 먼저 한 번 가입
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        // when — 동일 이메일로 재요청
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
        // given
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        // when — 동일 닉네임, 다른 이메일
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
        // given — 회원 존재
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        // when & then
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
        // given
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        // when & then
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

    /** 회원가입 → 로그인을 수행하고 응답 body에서 accessToken을 추출하는 헬퍼 */
    private String 회원가입_후_로그인_AT_추출() throws Exception {
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
        // given — 회원가입 + 로그인으로 AT 획득 (로그아웃 후 유저는 DB에 남음)
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
        // AT가 존재하지만 서명이 유효하지 않음 → jwt-error=TOKEN_INVALID → EntryPoint에서 "토큰이 만료되었습니다."
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 3단계 — 로그아웃 + 블랙리스트
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // 3-1. 로그아웃 → 200 OK + BL 키 생성 + RT 키 삭제
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3-1. 로그아웃 → 200 OK, AT 블랙리스트 등록, RT 삭제")
    void 로그아웃_성공() throws Exception {
        // given
        final String accessToken = 회원가입_후_로그인_AT_추출();
        final Claims claims = jwtProvider.parseToken(accessToken);
        final String userId = claims.getSubject();
        final String deviceId = claims.get("deviceId", String.class);

        // when
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // then — Redis: BL 키 생성 확인, RT 키 삭제 확인
        assertThat(tokenRedisService.isBlacklisted(accessToken)).isTrue();
        assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isNull();
    }

    // ─────────────────────────────────────────────────────────────
    // 3-2. 블랙리스트 AT 재사용 → 401
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3-2. 로그아웃된 AT 재사용 → 401, '토큰이 만료되었습니다.'")
    void 블랙리스트_AT_재사용_실패() throws Exception {
        // given — 로그아웃으로 AT를 블랙리스트에 등록
        final String accessToken = 회원가입_후_로그인_AT_추출();
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // when — 동일 AT로 인증 필요 엔드포인트 재요청
        // 블랙리스트 차단도 jwt-error=TOKEN_INVALID 분기 → "토큰이 만료되었습니다."
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 4단계 — 토큰 재발급 (RT Rotation)
    // ─────────────────────────────────────────────────────────────

    /** 회원가입 → 로그인 후 AT·RT를 함께 반환하는 헬퍼 */
    private record LoginTokens(String accessToken, String refreshToken) {}

    private LoginTokens 회원가입_후_로그인_토큰_추출() throws Exception {
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

    /**
     * 응답의 모든 Set-Cookie 헤더를 순회해 refreshToken 값만 파싱
     * getHeader()는 첫 번째 헤더만 반환하므로 getHeaders()로 전체를 확인
     */
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
        // given
        final LoginTokens tokens = 회원가입_후_로그인_토큰_추출();
        final Claims claims = jwtProvider.parseToken(tokens.accessToken());
        final String userId = claims.getSubject();
        final String deviceId = claims.get("deviceId", String.class);

        // when
        final MvcResult result = mockMvc.perform(post(REISSUE_URL)
                        .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        // then — 새 RT가 발급되어 기존 RT와 달라야 함 (RT Rotation 확인)
        final String newRt = setCookie에서_RT값_추출(result.getResponse());
        assertThat(newRt).isNotEqualTo(tokens.refreshToken());

        // Redis에 새 RT가 저장되어 있어야 함
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
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh Token 쿠키가 없습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 4-3. RT Reuse Detection → 401 + 전체 기기 RT 즉시 삭제
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("4-3. 구 RT 재사용 감지 → 401, 해당 유저 모든 RT 즉시 삭제")
    void RT_재사용_감지_실패() throws Exception {
        // given — 로그인 후 정상 재발급으로 구 RT를 무효화
        final LoginTokens tokens = 회원가입_후_로그인_토큰_추출();
        final Claims claims = jwtProvider.parseToken(tokens.accessToken());
        final String userId = claims.getSubject();
        final String deviceId = claims.get("deviceId", String.class);

        mockMvc.perform(post(REISSUE_URL)
                        .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                .andExpect(status().isOk());

        // when — 재발급 이전의 구 RT로 재요청 (Reuse Detection 트리거)
        mockMvc.perform(post(REISSUE_URL)
                        .cookie(new Cookie(CookieUtils.REFRESH_TOKEN_COOKIE_NAME, tokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다."));

        // then — 해당 유저의 모든 RT가 삭제되어야 함
        assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isNull();
    }

    // ─────────────────────────────────────────────────────────────
    // 7단계 — 엣지 케이스
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // 7-1. 소셜 계정으로 로컬 로그인 시도 → 400
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-1. 소셜 계정으로 로컬 로그인 시도 → 400, '소셜 로그인 전용 계정입니다.'")
    void 소셜_계정_로컬_로그인_실패() throws Exception {
        // given — socialProvider가 설정된 유저 직접 생성
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

        // when & then — 로컬 로그인 시도
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(SOCIAL_EMAIL, "anypassword"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("소셜 로그인 전용 계정입니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 7-2. 계정 상태별 로그인 차단 → 403
    // ─────────────────────────────────────────────────────────────

    static Stream<Arguments> 계정상태_메시지_매핑() {
        return Stream.of(
                Arguments.of(UserState.LOCKED,        "계정이 잠겨 있습니다."),
                Arguments.of(UserState.WITHDRAWN,     "탈퇴 처리 중인 계정입니다."),
                Arguments.of(UserState.PERMANENT_BAN, "정지된 계정입니다.")
        );
    }

    @ParameterizedTest(name = "7-2. {0} 상태 계정 로그인 → 403, {1}")
    @MethodSource("계정상태_메시지_매핑")
    void 계정상태별_로그인_차단(final UserState state, final String expectedMessage) throws Exception {
        // given — 회원가입 후 상태 변경
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        final User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        user.changeState(state);
        userRepository.save(user);

        // when & then
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    // ─────────────────────────────────────────────────────────────
    // 7-3. 유효하지 않은 MBTI ID로 회원가입 → 404
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-3. 유효하지 않은 MBTI ID로 회원가입 → 404, '존재하지 않는 MBTI입니다.'")
    void 유효하지_않은_MBTI_회원가입_실패() throws Exception {
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, "INVALID"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 MBTI입니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 7-4. 다중 기기 로그인 → 기기A 로그아웃 시 기기B RT 유지
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-4. 다중 기기 로그인 → 기기A 로그아웃 시 기기B RT 유지")
    void 다중_기기_로그인_독립_로그아웃() throws Exception {
        // given — 회원가입 후 두 번 로그인 (로그인마다 새 deviceId 발급)
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        final String atA = 로그인_후_AT_추출();
        final String atB = 로그인_후_AT_추출();

        final Claims claimsA = jwtProvider.parseToken(atA);
        final Claims claimsB = jwtProvider.parseToken(atB);
        final String userId    = claimsA.getSubject();
        final String deviceIdA = claimsA.get("deviceId", String.class);
        final String deviceIdB = claimsB.get("deviceId", String.class);

        // when — 기기A만 로그아웃
        mockMvc.perform(post(LOGOUT_URL)
                        .header("Authorization", "Bearer " + atA))
                .andExpect(status().isOk());

        // then — 기기A RT 삭제, 기기B RT 유지
        assertThat(tokenRedisService.getRefreshToken(userId, deviceIdA)).isNull();
        assertThat(tokenRedisService.getRefreshToken(userId, deviceIdB)).isNotNull();
    }

    /** 로그인만 수행하고 AT를 반환하는 헬퍼 (회원가입 선행 필요) */
    private String 로그인_후_AT_추출() throws Exception {
        final String body = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }
}
