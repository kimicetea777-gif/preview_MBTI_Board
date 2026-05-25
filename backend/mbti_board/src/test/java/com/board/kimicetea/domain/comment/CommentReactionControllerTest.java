package com.board.kimicetea.domain.comment;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.dto.CommentReactionRequest;
import com.board.kimicetea.domain.comment.repository.CommentReactionRepository;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 댓글 리액션 도메인 통합 테스트 (Phase 3 — 리액션 토글·취소)
 * 주의: like_count·dislike_count는 DB 트리거 관리 — flush+refresh 후 응답에 반영됨
 */
@AutoConfigureMockMvc
class CommentReactionControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired CommentReactionRepository commentReactionRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired StringRedisTemplate redisTemplate;
    @MockBean EmailService emailService;

    private static final String TEST_EMAIL    = "reaction-test@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME = "리액션테스터";
    private static final String TEST_MBTI     = "INFP";
    private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";

    @AfterEach
    void tearDown() {
        commentReactionRepository.deleteAll();
        commentRepository.deleteAll();
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

    /** 게시글 + 댓글을 한 번에 준비하고 댓글 ID 반환 */
    private long 게시글_및_댓글_준비_후_댓글ID_반환(final String accessToken) throws Exception {
        final String postBody = mockMvc.perform(post(POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new PostCreateRequest("테스트 제목", CONTENT, "FREE", null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final long postId = objectMapper.readTree(postBody).path("data").path("id").asLong();

        final String commentBody = mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("테스트 댓글", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(commentBody).path("data").path("id").asLong();
    }

    private void 리액션_등록(final long commentId, final String accessToken, final short type) throws Exception {
        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest(type))))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // 5단계 — 리액션 토글 (POST /api/comments/{commentId}/reactions)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("5-1. 좋아요 신규 등록 → 200, myReactionType=1, likeCount=1")
    void 리액션_좋아요_신규_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.dislikeCount").value(0));
    }

    @Test
    @DisplayName("5-2. 싫어요 신규 등록 → 200, myReactionType=-1, dislikeCount=1")
    void 리액션_싫어요_신규_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) -1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").value(-1))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.dislikeCount").value(1));
    }

    @Test
    @DisplayName("5-3. 좋아요 → 같은 좋아요 재등록 (취소) → 200, myReactionType=null, likeCount=0")
    void 리액션_좋아요_재등록_취소_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) 1);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").doesNotExist())
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("5-4. 싫어요 → 같은 싫어요 재등록 (취소) → 200, myReactionType=null, dislikeCount=0")
    void 리액션_싫어요_재등록_취소_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) -1);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) -1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").doesNotExist())
                .andExpect(jsonPath("$.data.dislikeCount").value(0));
    }

    @Test
    @DisplayName("5-5. 좋아요 → 싫어요 전환 → 200, myReactionType=-1, likeCount=0, dislikeCount=1")
    void 리액션_좋아요_싫어요_전환_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) 1);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) -1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").value(-1))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.dislikeCount").value(1));
    }

    @Test
    @DisplayName("5-6. 싫어요 → 좋아요 전환 → 200, myReactionType=1, dislikeCount=0, likeCount=1")
    void 리액션_싫어요_좋아요_전환_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) -1);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.dislikeCount").value(0));
    }

    @Test
    @DisplayName("5-7. 비인증 접근 → 401 Unauthorized")
    void 리액션_비인증_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5-8. 존재하지 않는 댓글 → 404, '댓글을 찾을 수 없습니다.'")
    void 리액션_없는_댓글_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(post(REACTIONS_BASE + "99999/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("5-9. reactionType=0 (유효하지 않은 값) → 400, '리액션 타입은 1(좋아요) 또는 -1(싫어요)이어야 합니다.'")
    void 리액션_잘못된_타입_0_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("리액션 타입은 1(좋아요) 또는 -1(싫어요)이어야 합니다."));
    }

    @Test
    @DisplayName("5-10. reactionType=null → 400 Bad Request (@NotNull 검증)")
    void 리액션_타입_null_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest(null))))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // 6단계 — 리액션 취소 (DELETE /api/comments/{commentId}/reactions)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("6-1. 좋아요 있을 때 취소 → 200, myReactionType=null, likeCount=0")
    void 리액션_취소_좋아요_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) 1);

        mockMvc.perform(delete(REACTIONS_BASE + commentId + "/reactions")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").doesNotExist())
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("6-2. 싫어요 있을 때 취소 → 200, myReactionType=null, dislikeCount=0")
    void 리액션_취소_싫어요_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        리액션_등록(commentId, at, (short) -1);

        mockMvc.perform(delete(REACTIONS_BASE + commentId + "/reactions")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").doesNotExist())
                .andExpect(jsonPath("$.data.dislikeCount").value(0));
    }

    @Test
    @DisplayName("6-3. 리액션 없을 때 취소 (멱등) → 200, myReactionType=null")
    void 리액션_취소_없는_경우_멱등_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(delete(REACTIONS_BASE + commentId + "/reactions")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myReactionType").doesNotExist());
    }

    @Test
    @DisplayName("6-4. 비인증 접근 → 401 Unauthorized")
    void 리액션_취소_비인증_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();
        final long commentId = 게시글_및_댓글_준비_후_댓글ID_반환(at);

        mockMvc.perform(delete(REACTIONS_BASE + commentId + "/reactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("6-5. 존재하지 않는 댓글 → 404, '댓글을 찾을 수 없습니다.'")
    void 리액션_취소_없는_댓글_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출();

        mockMvc.perform(delete(REACTIONS_BASE + "99999/reactions")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }
}
