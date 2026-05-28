package com.board.kimicetea.domain.mbti;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.mbti.dto.MbtiTestSubmitRequest;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.mail.EmailService;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MBTI 검사 문항 조회 및 결과 제출 통합 테스트 (Phase 6-1, 6-2)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 * 주의: 6-1 테스트는 정적 데이터만 조회, 6-2 테스트는 User/MbtiTestHistory 생성
 */
@AutoConfigureMockMvc
class MbtiTestControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MbtiTestHistoryRepository mbtiTestHistoryRepository;
    @Autowired
    TokenRedisService tokenRedisService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @MockBean
    EmailService emailService;

    private static final String TEST_EMAIL = "mbti-test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "MBTI테스터";
    private static final String TEST_MBTI = "INFP";

    @AfterEach
    void tearDown() {
        mbtiTestHistoryRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    /** 회원가입 → 로그인 → AT 추출 */
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

    /** MBTI 검사 제출 — eiScore=5, snScore=3, tfScore=2, jpScore=4 → ESTJ */
    private void MBTI_검사_제출(final String accessToken) throws Exception {
        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(
                        new MbtiTestSubmitRequest((short) 5, (short) 3, (short) 2, (short) 4))))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // 6-1. 검사 문항 조회 (GET /api/mbti/test/questions)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-1-1. 인증 없이 문항 조회 → 200, 32개 문항 반환")
    void 검사_문항_조회_비인증_성공() throws Exception {
        mockMvc.perform(get(MBTI_TEST_QUESTIONS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(32));
    }

    // ─────────────────────────────────────────────────────────────
    // 6-2. 검사 결과 제출 (POST /api/mbti/test/submit)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-2-1. 회원 정상 점수 제출 (ESTJ) → 200, mbtiId=ESTJ 반환")
    void 검사_결과_제출_회원_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + at)
                .content(objectMapper.writeValueAsString(
                        new MbtiTestSubmitRequest((short) 5, (short) 3, (short) 2, (short) 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mbtiId").value("ESTJ"))
                .andExpect(jsonPath("$.data.eiScore").value(5))
                .andExpect(jsonPath("$.data.snScore").value(3))
                .andExpect(jsonPath("$.data.tfScore").value(2))
                .andExpect(jsonPath("$.data.jpScore").value(4));
    }

    @Test
    @DisplayName("6-2-2. 비인증 접근 → 401 Unauthorized")
    void 검사_결과_제출_비인증_실패() throws Exception {
        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new MbtiTestSubmitRequest((short) 5, (short) 3, (short) 2, (short) 4))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6-2-3. eiScore=17 (범위 초과) → 400 Bad Request")
    void 검사_결과_제출_eiScore_범위초과_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + at)
                .content("{\"eiScore\":17,\"snScore\":3,\"tfScore\":2,\"jpScore\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6-2-4. eiScore=-17 (범위 미만) → 400 Bad Request")
    void 검사_결과_제출_eiScore_범위미만_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + at)
                .content("{\"eiScore\":-17,\"snScore\":3,\"tfScore\":2,\"jpScore\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6-2-5. snScore null → 400 Bad Request")
    void 검사_결과_제출_snScore_null_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(post(MBTI_SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + at)
                .content("{\"eiScore\":5,\"snScore\":null,\"tfScore\":2,\"jpScore\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6-2-6. 제출 후 내 프로필 조회 → mbtiVerified=TESTED, mbtiId=ESTJ")
    void 검사_결과_제출_후_프로필_mbtiVerified_변경() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        MBTI_검사_제출(at);

        mockMvc.perform(get(ME_URL)
                .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mbtiVerified").value("TESTED"))
                .andExpect(jsonPath("$.data.mbtiId").value("ESTJ"));
    }

    @Test
    @DisplayName("6-2-7. 제출 후 이력 조회 → length=1, 제출 점수·mbtiId 반영")
    void 검사_결과_제출_후_이력_저장_확인() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        MBTI_검사_제출(at);

        mockMvc.perform(get(MBTI_HISTORY_URL)
                .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].mbtiId").value("ESTJ"))
                .andExpect(jsonPath("$.data[0].eiScore").value(5))
                .andExpect(jsonPath("$.data[0].snScore").value(3))
                .andExpect(jsonPath("$.data[0].tfScore").value(2))
                .andExpect(jsonPath("$.data[0].jpScore").value(4));
    }
}
