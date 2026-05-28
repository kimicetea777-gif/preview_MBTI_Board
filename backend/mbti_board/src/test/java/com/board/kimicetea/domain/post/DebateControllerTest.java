package com.board.kimicetea.domain.post;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.post.dto.DebatePostCreateRequest;
import com.board.kimicetea.domain.post.repository.DebatePostRepository;
import com.board.kimicetea.domain.post.repository.PostRepository;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 토론 게시판 통합 테스트 (Phase 5 — POST /api/posts/debate)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 */
@AutoConfigureMockMvc
class DebateControllerTest extends IntegrationTestSupport {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    DebatePostRepository debatePostRepository;
    @Autowired
    TokenRedisService tokenRedisService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @MockBean
    EmailService emailService;

    private static final String TEST_EMAIL = "debate-test@example.com";
    private static final String TEST_EMAIL2 = "debate-test2@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "토론테스터";
    private static final String TEST_NICKNAME2 = "토론테스터2";
    private static final String TEST_MBTI = "INFP";
    private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";

    @AfterEach
    void tearDown() {
        // FK: debate_posts → posts → users
        postRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private String 회원가입_후_로그인_AT_추출(final String email, final String nickname) throws Exception {
        tokenRedisService.markEmailVerified(email);
        mockMvc.perform(post(SIGNUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(email, TEST_PASSWORD, nickname, TEST_MBTI))))
                .andExpect(status().isCreated());

        final String body = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, TEST_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    private long 토론_게시글_작성_후_ID_추출(final String accessToken) throws Exception {
        final String body = mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("토론 제목", CONTENT, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    // ─────────────────────────────────────────────────────────────
    // 1단계 — 토론 게시글 작성 (POST /api/posts/debate)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-1. 회원 토론 게시글 작성 (기본값) → 201, proLabel=찬성, conLabel=반대, isEnded=false")
    void 토론_게시글_작성_회원_기본값_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("토론 제목", CONTENT, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("토론 제목"))
                .andExpect(jsonPath("$.data.debateResult.proLabel").value("찬성"))
                .andExpect(jsonPath("$.data.debateResult.conLabel").value("반대"))
                .andExpect(jsonPath("$.data.debateResult.isClosed").value(false))
                .andExpect(jsonPath("$.data.isOwner").value(true))
                .andExpect(jsonPath("$.data.isGuest").value(false));
    }

    @Test
    @DisplayName("1-2. 회원 토론 게시글 작성 (proLabel·conLabel 직접 지정) → 201, 지정 라벨 반환")
    void 토론_게시글_작성_회원_커스텀_라벨_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("좋아 vs 싫어", CONTENT, null, null, "좋아", "싫어"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.debateResult.proLabel").value("좋아"))
                .andExpect(jsonPath("$.data.debateResult.conLabel").value("싫어"));
    }

    @Test
    @DisplayName("1-3. 비회원 토론 게시글 작성 시도 → 401 Unauthorized")
    void 토론_게시글_작성_비회원_인증_실패() throws Exception {
        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("비회원 토론", CONTENT, null, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("1-4. endsAt이 과거 시각 → 400, '마감일은 현재 시각보다 이후여야 합니다.'")
    void 토론_게시글_작성_과거_마감일_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("토론 제목", CONTENT, null,
                                        Instant.now().minusSeconds(60), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("마감일은 현재 시각보다 이후여야 합니다."));
    }

    @Test
    @DisplayName("1-5. endsAt 없음 (무기한 토론) → 201, endsAt=null, isEnded=false")
    void 토론_게시글_작성_무기한_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("무기한 토론", CONTENT, null, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.debateResult.endsAt").doesNotExist())
                .andExpect(jsonPath("$.data.debateResult.isClosed").value(false));
    }

    @Test
    @DisplayName("1-6. title 공백으로 작성 → 400 Bad Request")
    void 토론_게시글_작성_title_공백_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("   ", CONTENT, null, null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("1-7. targetMbti 지정 작성 성공 → 201, targetMbtiId 반환")
    void 토론_게시글_작성_targetMbti_지정_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("INFP에게", CONTENT, "INFP", null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.targetMbtiId").value("INFP"));
    }

    // ─────────────────────────────────────────────────────────────
    // 2단계 — 토론 게시글 단건 조회 (GET /api/posts/{id})
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2-1. 토론 게시글 단건 조회 → 200, debateResult 포함")
    void 토론_게시글_단건_조회_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long id = 토론_게시글_작성_후_ID_추출(at);

        mockMvc.perform(get(POSTS_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.debateResult").exists())
                .andExpect(jsonPath("$.data.debateResult.proLabel").value("찬성"))
                .andExpect(jsonPath("$.data.debateResult.conLabel").value("반대"));
    }

    @Test
    @DisplayName("2-2. 마감된 토론 게시글 단건 조회 → 200, isEnded=true")
    void 토론_게시글_단건_조회_마감_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        final String body = mockMvc.perform(post(DEBATE_POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new DebatePostCreateRequest("마감 토론", CONTENT, null,
                                        Instant.now().plusSeconds(1), null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final long id = objectMapper.readTree(body).path("data").path("id").asLong();

        Thread.sleep(1500);

        mockMvc.perform(get(POSTS_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.debateResult.isClosed").value(true));
    }

    // ─────────────────────────────────────────────────────────────
    // 3단계 — 토론 게시글 수정·삭제 (PATCH / DELETE /api/posts/{id})
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3-1. 회원 본인 토론 게시글 수정 → 200, 변경된 title 반환")
    void 토론_게시글_수정_회원_본인_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long id = 토론_게시글_작성_후_ID_추출(at);

        mockMvc.perform(patch(POSTS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new com.board.kimicetea.domain.post.dto.PostUpdateRequest("수정된 토론 제목", CONTENT,
                                        null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 토론 제목"))
                .andExpect(jsonPath("$.data.debateResult").exists());
    }

    @Test
    @DisplayName("3-2. 다른 회원이 토론 게시글 수정 → 403, '게시글 수정/삭제 권한이 없습니다.'")
    void 토론_게시글_수정_다른_회원_실패() throws Exception {
        final String at1 = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final String at2 = 회원가입_후_로그인_AT_추출(TEST_EMAIL2, TEST_NICKNAME2);
        final long id = 토론_게시글_작성_후_ID_추출(at1);

        mockMvc.perform(patch(POSTS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at2)
                        .content(objectMapper.writeValueAsString(
                                new com.board.kimicetea.domain.post.dto.PostUpdateRequest("수정 시도", CONTENT, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("게시글 수정/삭제 권한이 없습니다."));
    }

    @Test
    @DisplayName("3-3. 회원 본인 토론 게시글 삭제 → 200, 이후 단건 조회 404")
    void 토론_게시글_삭제_회원_본인_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long id = 토론_게시글_작성_후_ID_추출(at);

        mockMvc.perform(delete(POSTS_URL + "/" + id)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get(POSTS_URL + "/" + id))
                .andExpect(status().isNotFound());
    }
}
