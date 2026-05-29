package com.board.kimicetea.domain.user;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.dto.VotePostCreateRequest;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.user.dto.ChangePasswordRequest;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.WithdrawalRequest;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 유저 도메인 통합 테스트 (8단계)
 * 주의: EmailService는 @MockBean으로 대체 — OTP는 Redis에 저장되므로 테스트에서 직접 읽어 사용
 */
@AutoConfigureMockMvc
class UserControllerTest extends IntegrationTestSupport {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;
        @Autowired
        UserRepository userRepository;
        @Autowired
        PostRepository postRepository;
        @Autowired
        MbtiMetadataRepository mbtiMetadataRepository;
        @Autowired
        TokenRedisService tokenRedisService;
        @Autowired
        JwtProvider jwtProvider;
        @Autowired
        StringRedisTemplate redisTemplate;
        @MockBean
        EmailService emailService;

        private static final String TEST_EMAIL = "user-test@example.com";
        private static final String TEST_PASSWORD = "password123!";
        private static final String TEST_NICKNAME = "유저테스트";
        private static final String TEST_MBTI = "INTJ";

        private static final String OTHER_EMAIL = "other-user-test@example.com";
        private static final String OTHER_NICKNAME = "다른유저닉";

        private static final String SOCIAL_EMAIL = "social-user-test@example.com";

        @AfterEach
        void tearDown() {
                postRepository.deleteAll(); // FK(user_id) ON DELETE SET NULL — 게시글 먼저 삭제
                userRepository.deleteAll();
                redisTemplate.execute((RedisCallback<Void>) connection -> {
                        connection.serverCommands().flushAll();
                        return null;
                });
        }

        private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";

        private static final List<VoteOptionRequest> TWO_OPTIONS = List.of(
                        new VoteOptionRequest("선택지 A", null),
                        new VoteOptionRequest("선택지 B", null));

        // ─────────────────────────────────────────────────────────────
        // 헬퍼
        // ─────────────────────────────────────────────────────────────

        /** 회원가입 → 로그인 후 AT를 반환하는 헬퍼 */
        private String 회원가입_후_로그인_AT_추출() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME,
                                                                TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("accessToken").asText();
        }

        /**
         * OTP 발송 요청 후 Redis에서 OTP 값을 읽어 반환하는 헬퍼
         * EmailService는 @MockBean이므로 실제 이메일은 발송되지 않으나 Redis에는 저장됨
         */
        private String OTP_발송_후_코드_추출(final String accessToken) throws Exception {
                final String userId = jwtProvider.parseToken(accessToken).getSubject();

                mockMvc.perform(post(WITHDRAWAL_VERIFY_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());

                return redisTemplate.opsForValue().get("WD:" + userId);
        }

        /** FREE 게시글 작성 헬퍼 */
        private void FREE_게시글_작성(final String accessToken, final String title) throws Exception {
                mockMvc.perform(post(POSTS_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest(title, CONTENT, "FREE", null, null))))
                                .andExpect(status().isCreated());
        }

        /** VOTE 게시글 작성 헬퍼 */
        private void VOTE_게시글_작성(final String accessToken) throws Exception {
                mockMvc.perform(post(VOTE_POSTS_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new VotePostCreateRequest("투표 제목", CONTENT, null, TWO_OPTIONS, null))))
                                .andExpect(status().isCreated());
        }

        // ─────────────────────────────────────────────────────────────
        // 8-1. 내 프로필 조회 성공 → 200 OK
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-1. 내 프로필 조회 → 200 OK, 민감 정보(email·role·state) 포함")
        void 내_프로필_조회_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(get(ME_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                                .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME))
                                .andExpect(jsonPath("$.data.role").value("USER"))
                                .andExpect(jsonPath("$.data.state").value("NORMAL"))
                                .andExpect(jsonPath("$.data.isSocialOnly").value(false))
                                .andExpect(jsonPath("$.data.mbtiId").value(TEST_MBTI));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-2. AT 없이 내 프로필 요청 → 401
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-2. AT 없이 내 프로필 요청 → 401, '인증이 필요합니다.'")
        void 내_프로필_조회_인증_없음_실패() throws Exception {
                mockMvc.perform(get(ME_URL))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-3. 닉네임 변경 성공 → 200 OK
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-3. 닉네임 변경 → 200 OK, 응답에 변경된 닉네임 반환")
        void 닉네임_변경_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateProfileRequest("변경된닉네임", null, null))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.nickname").value("변경된닉네임"));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-4. MBTI 변경 성공 → 200 OK, mbtiVerified = SELF
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-4. MBTI 변경 → 200 OK, mbtiId 반영·mbtiVerified = SELF")
        void MBTI_변경_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateProfileRequest(null, null, "INFP"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.mbtiId").value("INFP"))
                                .andExpect(jsonPath("$.data.mbtiVerified").value("SELF"));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-5. 중복 닉네임으로 변경 시도 → 409 Conflict
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-5. 이미 사용 중인 닉네임으로 변경 → 409, '이미 사용 중인 닉네임입니다.'")
        void 중복_닉네임_변경_실패() throws Exception {
                tokenRedisService.markEmailVerified(OTHER_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(OTHER_EMAIL, TEST_PASSWORD, OTHER_NICKNAME,
                                                                TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateProfileRequest(OTHER_NICKNAME, null, null))))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-6. 존재하지 않는 MBTI ID로 변경 시도 → 404
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-6. 존재하지 않는 MBTI ID로 변경 → 404, '존재하지 않는 MBTI입니다.'")
        void 없는_MBTI_변경_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateProfileRequest(null, null, "INVALID"))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("존재하지 않는 MBTI입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-7. 공개 프로필 조회 (인증 없이) → 200 OK, 민감 정보 제외
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-7. 공개 프로필 조회 (비인증) → 200 OK, email·state 필드 없음")
        void 공개_프로필_조회_성공() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME,
                                                                TEST_MBTI))))
                                .andExpect(status().isCreated());

                final UUID userId = userRepository.findByEmail(TEST_EMAIL).orElseThrow().getId();

                mockMvc.perform(get(USER_URL + userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                                .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME))
                                .andExpect(jsonPath("$.data.mbtiId").value(TEST_MBTI))
                                .andExpect(jsonPath("$.data.email").doesNotExist())
                                .andExpect(jsonPath("$.data.state").doesNotExist());
        }

        // ─────────────────────────────────────────────────────────────
        // 8-8. 존재하지 않는 유저 공개 프로필 조회 → 404
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-8. 존재하지 않는 유저 공개 프로필 조회 → 404, '존재하지 않는 사용자입니다.'")
        void 공개_프로필_조회_없는_유저_실패() throws Exception {
                mockMvc.perform(get(USER_URL + UUID.randomUUID()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-9. 탈퇴 OTP 발송 → 200 OK, Redis WD 키 생성
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-9. 탈퇴 OTP 발송 → 200 OK, Redis WD:{userId} 키 생성·6자리 코드")
        void 탈퇴_OTP_발송_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                final String userId = jwtProvider.parseToken(accessToken).getSubject();

                mockMvc.perform(post(WITHDRAWAL_VERIFY_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."));

                final String otp = redisTemplate.opsForValue().get("WD:" + userId);
                assertThat(otp).isNotNull().hasSize(6).containsOnlyDigits();
        }

        // ─────────────────────────────────────────────────────────────
        // 8-10. OTP 검증 후 탈퇴 성공 → Redis RT 삭제·AT 블랙리스트
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-10. OTP 검증 후 탈퇴 → 200 OK, Redis RT 삭제·AT 블랙리스트 등록")
        void 회원_탈퇴_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                final Claims claims = jwtProvider.parseToken(accessToken);
                final String userId = claims.getSubject();
                final String deviceId = claims.get("deviceId", String.class);
                final String otp = OTP_발송_후_코드_추출(accessToken);

                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest(otp))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("탈퇴 처리가 완료되었습니다."));

                assertThat(tokenRedisService.getRefreshToken(userId, deviceId)).isNull();
                assertThat(tokenRedisService.isBlacklisted(accessToken)).isTrue();
        }

        // ─────────────────────────────────────────────────────────────
        // 8-11. 탈퇴 후 동일 AT 재사용 → 401 (블랙리스트 차단)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-11. 탈퇴 후 동일 AT 재사용 → 401, '토큰이 만료되었습니다.'")
        void 탈퇴_후_AT_재사용_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                final String otp = OTP_발송_후_코드_추출(accessToken);

                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest(otp))))
                                .andExpect(status().isOk());

                mockMvc.perform(get(ME_URL)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-12. 비밀번호 변경 성공 → 200 OK
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-12. 비밀번호 변경 → 200 OK, '비밀번호가 변경되었습니다.'")
        void 비밀번호_변경_성공() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(PASSWORD_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new ChangePasswordRequest(TEST_PASSWORD, "newpassword123!"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-13. 현재 비밀번호 불일치 → 400 Bad Request
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-13. 현재 비밀번호 불일치 → 400, '현재 비밀번호가 올바르지 않습니다.'")
        void 비밀번호_변경_현재_비밀번호_틀림_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(patch(PASSWORD_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new ChangePasswordRequest("wrongpassword!", "newpassword123!"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("현재 비밀번호가 올바르지 않습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-14. 소셜 전용 계정 비밀번호 변경 시도 → 400 Bad Request
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-14. 소셜 전용 계정 비밀번호 변경 시도 → 400, '소셜 로그인 전용 계정입니다.'")
        void 소셜_계정_비밀번호_변경_실패() throws Exception {
                final User socialUser = User.builder()
                                .email(SOCIAL_EMAIL)
                                .nickname("소셜유저테스트")
                                .socialProvider("google")
                                .providerId("test-provider-google-002")
                                .mbti(mbtiMetadataRepository.findById("NONE").orElseThrow())
                                .mbtiVerified(MbtiVerified.DEFAULT)
                                .role(UserRole.USER)
                                .state(UserState.NORMAL)
                                .build();
                final User saved = userRepository.save(socialUser);

                final String deviceId = UUID.randomUUID().toString();
                final String accessToken = jwtProvider.generateAccessToken(
                                saved.getId(), saved.getEmail(), saved.getRole().name(), deviceId);

                mockMvc.perform(patch(PASSWORD_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new ChangePasswordRequest("anypassword!", "newpassword123!"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("소셜 로그인 전용 계정입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-15. 잘못된 OTP로 탈퇴 → 400 Bad Request
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-15. 잘못된 OTP 코드로 탈퇴 → 400, '인증 코드가 올바르지 않습니다.'")
        void 잘못된_OTP_탈퇴_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                OTP_발송_후_코드_추출(accessToken); // OTP 발급 (Redis에 저장)

                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest("000000"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-16. OTP 미발급 상태로 탈퇴 → 400 Bad Request (만료)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-16. OTP 발급 없이 탈퇴 시도 → 400, '인증 코드가 만료되었습니다.'")
        void OTP_없이_탈퇴_실패() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest("123456"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다. 다시 요청해 주세요."));
        }

        // ─────────────────────────────────────────────────────────────
        // 8-17. OTP 3회 오답 → OTP 무효화, 새 OTP 요청 필요
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("8-17. OTP 3회 오답 → 400, '인증 코드를 3회 잘못 입력하였습니다.' + OTP 무효화")
        void OTP_3회_오답_OTP_무효화() throws Exception {
                final String accessToken = 회원가입_후_로그인_AT_추출();
                OTP_발송_후_코드_추출(accessToken); // OTP 발급

                // 1·2회 오답 — 아직 OTP 유효
                for (int i = 0; i < 2; i++) {
                        mockMvc.perform(delete(ME_URL)
                                        .header("Authorization", "Bearer " + accessToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(new WithdrawalRequest("000000"))))
                                        .andExpect(status().isBadRequest())
                                        .andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));
                }

                // 3회 오답 — OTP 무효화
                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest("000000"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해 주세요."));

                // OTP가 삭제됐으므로 정상 코드로 시도해도 만료 오류
                mockMvc.perform(delete(ME_URL)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new WithdrawalRequest("123456"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다. 다시 요청해 주세요."));
        }

        // ═════════════════════════════════════════════════════════════
        // Phase 9 — 내가 작성한 게시글 목록 (GET /api/users/me/posts)
        // ═════════════════════════════════════════════════════════════

        // ─────────────────────────────────────────────────────────────
        // 9-1. 내 게시글 목록 전체 조회 → 200, totalElements=2
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-1. 내 게시글 2개 작성 후 전체 조회 → 200, totalElements=2")
        void 내_게시글_목록_전체_조회_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                FREE_게시글_작성(at, "첫 번째 글");
                FREE_게시글_작성(at, "두 번째 글");

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.totalElements").value(2))
                                .andExpect(jsonPath("$.data.content").isArray());
        }

        // ─────────────────────────────────────────────────────────────
        // 9-2. AT 없이 요청 → 401
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-2. AT 없이 내 게시글 목록 요청 → 401, '인증이 필요합니다.'")
        void 내_게시글_목록_인증_없음_실패() throws Exception {
                mockMvc.perform(get(MY_POSTS_URL))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-3. type=FREE 필터 → FREE 게시글만 반환
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-3. FREE 1개·VOTE 1개 작성 후 type=FREE 조회 → totalElements=1, postType=FREE")
        void 내_게시글_목록_type_FREE_필터_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                FREE_게시글_작성(at, "자유글");
                VOTE_게시글_작성(at);

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .param("type", "FREE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].typeCode").value("FREE"));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-4. type=VOTE 필터 → VOTE 게시글만 반환
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-4. FREE 1개·VOTE 1개 작성 후 type=VOTE 조회 → totalElements=1, postType=VOTE")
        void 내_게시글_목록_type_VOTE_필터_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                FREE_게시글_작성(at, "자유글");
                VOTE_게시글_작성(at);

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .param("type", "VOTE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].typeCode").value("VOTE"));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-5. 타인 게시글은 내 목록에 포함되지 않음
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-5. 내 게시글 1개 + 타인 게시글 1개 → 내 목록 totalElements=1")
        void 내_게시글_목록_타인_게시글_미포함_성공() throws Exception {
                final String myAt = 회원가입_후_로그인_AT_추출();

                // 타인 가입 및 게시글 작성
                tokenRedisService.markEmailVerified(OTHER_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(OTHER_EMAIL, TEST_PASSWORD, OTHER_NICKNAME, TEST_MBTI))))
                                .andExpect(status().isCreated());
                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LoginRequest(OTHER_EMAIL, TEST_PASSWORD))))
                                .andReturn().getResponse().getContentAsString();
                final String otherAt = objectMapper.readTree(body).path("data").path("accessToken").asText();
                FREE_게시글_작성(otherAt, "타인 글");

                FREE_게시글_작성(myAt, "내 글");

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + myAt))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].title").value("내 글"));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-6. 내 게시글 없을 때 → 200, 빈 목록
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-6. 게시글 없는 회원 조회 → 200, 빈 content")
        void 내_게시글_목록_비어있음_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(0))
                                .andExpect(jsonPath("$.data.content").isEmpty());
        }

        // ─────────────────────────────────────────────────────────────
        // 9-7. type 소문자 입력 → 대소문자 정규화 후 정상 조회
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-7. type=free (소문자) 입력 → 대소문자 정규화, 200 OK")
        void 내_게시글_목록_type_소문자_정규화_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                FREE_게시글_작성(at, "소문자 타입 테스트");

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .param("type", "free"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-8. type=INVALID → 400, '존재하지 않는 게시판 유형입니다.'
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-8. type=INVALID → 400, '존재하지 않는 게시판 유형입니다.'")
        void 내_게시글_목록_유효하지_않은_type_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .param("type", "INVALID"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("존재하지 않는 게시판 유형입니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 9-9. page=-1 → 400 Bad Request
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-9. page=-1 → 400 Bad Request")
        void 내_게시글_목록_page_음수_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .param("page", "-1"))
                                .andExpect(status().isBadRequest());
        }

        // ─────────────────────────────────────────────────────────────
        // 9-10. 소프트 삭제된 내 게시글은 목록에 미포함
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("9-10. 내 게시글 2개 중 1개 삭제 후 조회 → totalElements=1")
        void 내_게시글_목록_소프트_삭제_미포함_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                FREE_게시글_작성(at, "남길 글");

                // 두 번째 글 작성 후 ID 추출하여 삭제
                final String created = mockMvc.perform(post(POSTS_URL)
                                .header("Authorization", "Bearer " + at)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("삭제할 글", CONTENT, "FREE", null, null))))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                final long deletedId = objectMapper.readTree(created).path("data").path("id").asLong();

                mockMvc.perform(delete(POSTS_URL + "/" + deletedId)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk());

                mockMvc.perform(get(MY_POSTS_URL)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].title").value("남길 글"));
        }
}
