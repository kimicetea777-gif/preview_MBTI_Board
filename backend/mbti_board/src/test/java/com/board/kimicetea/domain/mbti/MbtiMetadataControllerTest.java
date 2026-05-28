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
 * MBTI 검사 이력 조회 및 메타데이터 API 통합 테스트 (Phase 6-3, 6-4, 6-5)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 * 주의: mbti_metadata는 Flyway 시드 데이터(17종) 사용 — tearDown에서 삭제하지 않음
 */
@AutoConfigureMockMvc
class MbtiMetadataControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired MbtiTestHistoryRepository mbtiTestHistoryRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired StringRedisTemplate redisTemplate;
    @MockBean EmailService emailService;

    private static final String TEST_EMAIL    = "mbti-meta-test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "MBTI메타테스터";
    private static final String TEST_MBTI     = "INFP";

    @AfterEach
    void tearDown() {
        // Flyway 시드 데이터(V5 users+posts)를 건드리지 않도록 테스트 생성 유저만 삭제
        // mbti_test_history는 ON DELETE CASCADE 로 함께 제거됨
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
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

    /** MBTI 검사 제출 — scores → MBTI 재도출 */
    private void MBTI_검사_제출(final String accessToken, final short ei, final short sn, final short tf, final short jp) throws Exception {
        mockMvc.perform(post(MBTI_SUBMIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new MbtiTestSubmitRequest(ei, sn, tf, jp))))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // 6-3. 검사 이력 조회 (GET /api/users/me/mbti/history)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-3-1. 인증 유저 — 이력 없을 때 조회 → 200, 빈 배열")
    void 이력_조회_이력_없음_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(get(MBTI_HISTORY_URL)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("6-3-2. 인증 유저 — 이력 1개 조회 → mbtiId·4개 점수·createdAt 포함")
    void 이력_조회_이력_1개_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        // eiScore=5, snScore=3, tfScore=2, jpScore=4 → ESTJ
        MBTI_검사_제출(at, (short) 5, (short) 3, (short) 2, (short) 4);

        mockMvc.perform(get(MBTI_HISTORY_URL)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].mbtiId").value("ESTJ"))
                .andExpect(jsonPath("$.data[0].eiScore").value(5))
                .andExpect(jsonPath("$.data[0].snScore").value(3))
                .andExpect(jsonPath("$.data[0].tfScore").value(2))
                .andExpect(jsonPath("$.data[0].jpScore").value(4))
                .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("6-3-3. 인증 유저 — 복수 이력 최신순 정렬 확인")
    void 이력_조회_복수_이력_최신순_정렬() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        // 첫 번째 제출: eiScore=5 → ESTJ
        MBTI_검사_제출(at, (short) 5, (short) 3, (short) 2, (short) 4);
        // 두 번째 제출: eiScore=-5 → INFP
        MBTI_검사_제출(at, (short) -5, (short) -3, (short) -2, (short) -4);

        mockMvc.perform(get(MBTI_HISTORY_URL)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].mbtiId").value("INFP"))  // 최신 이력이 [0]
                .andExpect(jsonPath("$.data[1].mbtiId").value("ESTJ"));
    }

    @Test
    @DisplayName("6-3-4. 비인증 접근 → 401 Unauthorized")
    void 이력_조회_비인증_실패() throws Exception {
        mockMvc.perform(get(MBTI_HISTORY_URL))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // 6-4. MBTI 전체 목록 조회 (GET /api/mbti)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-4-1. 비인증 전체 목록 조회 → 200, 17종(MBTI 16종 + NONE) 반환")
    void MBTI_전체목록_조회_성공() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(17));
    }

    @Test
    @DisplayName("6-4-2. 응답 항목 구조 검증 → id·colorCode·description 존재")
    void MBTI_전체목록_항목_구조_검증() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data[0].colorCode").isNotEmpty())
                .andExpect(jsonPath("$.data[0].description").isNotEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // 6-5. MBTI 단건 조회 (GET /api/mbti/{id})
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-5-1. 유효한 ID로 단건 조회 (INTP) → 200, id=INTP·colorCode·description 반환")
    void MBTI_단건_조회_성공() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL + "/INTP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("INTP"))
                .andExpect(jsonPath("$.data.colorCode").value("#5B7FA6"))
                .andExpect(jsonPath("$.data.description").isNotEmpty());
    }

    @Test
    @DisplayName("6-5-2. NONE ID 단건 조회 → 200, id=NONE 반환")
    void MBTI_단건_조회_NONE_성공() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL + "/NONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("NONE"))
                .andExpect(jsonPath("$.data.colorCode").value("#A8A29E"));
    }

    @Test
    @DisplayName("6-5-3. 존재하지 않는 ID (XXXX) → 404, '존재하지 않는 MBTI입니다.'")
    void MBTI_단건_조회_없는_ID_실패() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL + "/XXXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 MBTI입니다."));
    }

    @Test
    @DisplayName("6-5-4. ID 7자 이상 (TOOLONG) → 400 Bad Request")
    void MBTI_단건_조회_ID_길이초과_실패() throws Exception {
        mockMvc.perform(get(MBTI_LIST_URL + "/TOOLONG"))
                .andExpect(status().isBadRequest());
    }
}
