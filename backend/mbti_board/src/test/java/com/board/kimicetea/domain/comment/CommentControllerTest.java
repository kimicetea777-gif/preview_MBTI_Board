package com.board.kimicetea.domain.comment;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.dto.CommentReactionRequest;
import com.board.kimicetea.domain.comment.dto.CommentUpdateRequest;
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
 * 댓글 도메인 통합 테스트 (Phase 3 — 댓글 CRUD)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 */
@AutoConfigureMockMvc
class CommentControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired CommentReactionRepository commentReactionRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired StringRedisTemplate redisTemplate;
    @MockBean EmailService emailService;

    private static final String TEST_EMAIL    = "comment-test@example.com";
    private static final String TEST_EMAIL2   = "comment-test2@example.com";
    private static final String TEST_PASSWORD = "password123!";
    private static final String TEST_NICKNAME  = "댓글테스터";
    private static final String TEST_NICKNAME2 = "댓글테스터2";
    private static final String TEST_MBTI     = "INFP";
    private static final String GUEST_PASSWORD = "guest1234";
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

    private long 게시글_작성_후_ID_추출(final String accessToken, final String type) throws Exception {
        final String body = mockMvc.perform(post(POSTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new PostCreateRequest("테스트 제목", CONTENT, type, null, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private long 회원_댓글_작성_후_ID_추출(final long postId, final String accessToken) throws Exception {
        final String body = mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("테스트 댓글", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private long 비회원_댓글_작성_후_ID_추출(final long postId, final String guestPassword) throws Exception {
        final String body = mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("비회원 댓글", null, guestPassword, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    // ─────────────────────────────────────────────────────────────
    // 1단계 — 댓글 목록 조회 (GET /api/posts/{postId}/comments)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-1. 댓글 없는 게시글 조회 → 200, 빈 배열")
    void 댓글_목록_조회_빈_결과_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("1-2. 루트 댓글만 있을 때 조회 → 200, replies=[]")
    void 댓글_목록_조회_루트만_replies_비어있음_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].replies").isArray())
                .andExpect(jsonPath("$.data[0].replies").isEmpty());
    }

    @Test
    @DisplayName("1-3. 루트 + 대댓글 구조 조회 → 200, 루트 replies에 대댓글 포함, 대댓글 replies=[]")
    void 댓글_목록_조회_계층형_구조_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long parentId = 회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("대댓글", parentId, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].replies[0].content").value("대댓글"))
                .andExpect(jsonPath("$.data[0].replies[0].replies").isArray())
                .andExpect(jsonPath("$.data[0].replies[0].replies").isEmpty());
    }

    @Test
    @DisplayName("1-4. 비인증 유저 조회 → 200, 모든 댓글 myReactionType=null")
    void 댓글_목록_조회_비인증_myReactionType_null() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].myReactionType").doesNotExist());
    }

    @Test
    @DisplayName("1-5. 리액션 있는 회원 조회 → 200, 해당 댓글 myReactionType=1")
    void 댓글_목록_조회_리액션_있는_회원_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at);

        // 리액션 등록
        mockMvc.perform(post(REACTIONS_BASE + commentId + "/reactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                .andExpect(status().isOk());

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].myReactionType").value(1));
    }

    @Test
    @DisplayName("1-6. 리액션 없는 회원 조회 → 200, 모든 댓글 myReactionType=null")
    void 댓글_목록_조회_리액션_없는_회원_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].myReactionType").doesNotExist());
    }

    @Test
    @DisplayName("1-7. 소프트 삭제된 댓글 → 200, 응답에 미포함")
    void 댓글_목록_조회_소프트삭제_미포함_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk());

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("1-8. 존재하지 않는 게시글 → 404, '게시글을 찾을 수 없습니다.'")
    void 댓글_목록_조회_없는_게시글_실패() throws Exception {
        mockMvc.perform(get(POSTS_URL + "/99999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 2단계 — 댓글 작성 (POST /api/posts/{postId}/comments)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2-1. 회원 루트 댓글 작성 → 201, isGuest=false, authorMbtiId=회원MBTI")
    void 댓글_작성_회원_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("회원 댓글", null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isGuest").value(false))
                .andExpect(jsonPath("$.data.authorMbtiId").value(TEST_MBTI));
    }

    @Test
    @DisplayName("2-2. 비회원 댓글 작성 → 201, isGuest=true, authorMbtiId=NONE")
    void 댓글_작성_비회원_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("비회원 댓글", null, GUEST_PASSWORD, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isGuest").value(true))
                .andExpect(jsonPath("$.data.authorMbtiId").value("NONE"));
    }

    @Test
    @DisplayName("2-3. 비회원 guestPassword 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
    void 댓글_작성_비회원_비밀번호_없음_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("비회원 댓글", null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
    }

    @Test
    @DisplayName("2-4. 대댓글 작성 (유효한 parentId) → 201, 목록 조회 시 루트 replies에 포함")
    void 댓글_작성_대댓글_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long parentId = 회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("대댓글", parentId, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].replies[0].content").value("대댓글"));
    }

    @Test
    @DisplayName("2-5. 대댓글의 대댓글 시도 → 400, '대댓글에는 댓글을 달 수 없습니다.'")
    void 댓글_작성_대댓글_깊이_초과_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long rootId = 회원_댓글_작성_후_ID_추출(postId, at);

        // 대댓글 생성
        final String replyBody = mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("대댓글", rootId, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final long replyId = objectMapper.readTree(replyBody).path("data").path("id").asLong();

        // 대댓글의 대댓글 시도
        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("대대댓글", replyId, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("대댓글에는 댓글을 달 수 없습니다."));
    }

    @Test
    @DisplayName("2-6. parentId가 다른 게시글 소속 댓글 ID → 404, '댓글을 찾을 수 없습니다.'")
    void 댓글_작성_다른_게시글_parentId_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long post1Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long post2Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentOnPost1 = 회원_댓글_작성_후_ID_추출(post1Id, at);

        mockMvc.perform(post(POSTS_URL + "/" + post2Id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("대댓글 시도", commentOnPost1, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("2-7. 존재하지 않는 게시글에 작성 → 404, '게시글을 찾을 수 없습니다.'")
    void 댓글_작성_없는_게시글_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

        mockMvc.perform(post(POSTS_URL + "/99999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("댓글", null, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("2-8. content 공백 → 400 Bad Request")
    void 댓글_작성_content_공백_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("   ", null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2-9. DEBATE 게시글 — debateStance 없이 작성 → 400, '토론 게시글 댓글은 찬반 입장(1 또는 -1)을 선택해야 합니다.'")
    void 댓글_작성_DEBATE_stance_없음_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "DEBATE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("토론 댓글", null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("토론 게시글 댓글은 찬반 입장(1 또는 -1)을 선택해야 합니다."));
    }

    @Test
    @DisplayName("2-10. DEBATE 게시글 — debateStance=1로 작성 → 201")
    void 댓글_작성_DEBATE_stance_지정_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "DEBATE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("찬성 댓글", null, null, (short) 1))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("2-11. FREE 게시글 — debateStance 포함 작성 → 400, '일반·투표 게시글 댓글에는 찬반 입장을 설정할 수 없습니다.'")
    void 댓글_작성_FREE_stance_포함_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentCreateRequest("댓글", null, null, (short) 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("일반·투표 게시글 댓글에는 찬반 입장을 설정할 수 없습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 3단계 — 댓글 수정 (PATCH /api/posts/{postId}/comments/{id})
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3-1. 회원 본인 수정 → 200, 변경된 content 반환")
    void 댓글_수정_회원_본인_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정된 댓글", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"));
    }

    @Test
    @DisplayName("3-2. 다른 회원이 수정 시도 → 403, '댓글 수정/삭제 권한이 없습니다.'")
    void 댓글_수정_다른_회원_실패() throws Exception {
        final String at1 = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final String at2 = 회원가입_후_로그인_AT_추출(TEST_EMAIL2, TEST_NICKNAME2);
        final long postId = 게시글_작성_후_ID_추출(at1, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at1);

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at2)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정 시도", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("댓글 수정/삭제 권한이 없습니다."));
    }

    @Test
    @DisplayName("3-3. 비회원 올바른 guestPassword → 200, 변경된 content 반환")
    void 댓글_수정_비회원_올바른_비밀번호_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정된 비회원 댓글", GUEST_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 비회원 댓글"));
    }

    @Test
    @DisplayName("3-4. 비회원 틀린 guestPassword → 403, '비밀번호가 일치하지 않습니다.'")
    void 댓글_수정_비회원_틀린_비밀번호_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정 시도", "wrongpassword"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("3-5. 비회원 guestPassword 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
    void 댓글_수정_비회원_비밀번호_없음_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정 시도", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
    }

    @Test
    @DisplayName("3-6. 존재하지 않는 댓글 수정 → 404, '댓글을 찾을 수 없습니다.'")
    void 댓글_수정_없는_댓글_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(patch(POSTS_URL + "/" + postId + "/comments/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정 시도", null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("3-7. 다른 게시글의 댓글 ID로 접근 → 404, '댓글을 찾을 수 없습니다.'")
    void 댓글_수정_다른_게시글_댓글_접근_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long post1Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long post2Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(post1Id, at);

        mockMvc.perform(patch(POSTS_URL + "/" + post2Id + "/comments/" + commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + at)
                        .content(objectMapper.writeValueAsString(
                                new CommentUpdateRequest("수정 시도", null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    // ─────────────────────────────────────────────────────────────
    // 4단계 — 댓글 삭제 (DELETE /api/posts/{postId}/comments/{id})
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("4-1. 회원 본인 삭제 → 200, 이후 목록 조회 시 해당 댓글 미포함")
    void 댓글_삭제_회원_본인_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("4-2. 다른 회원이 삭제 시도 → 403, '댓글 수정/삭제 권한이 없습니다.'")
    void 댓글_삭제_다른_회원_실패() throws Exception {
        final String at1 = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final String at2 = 회원가입_후_로그인_AT_추출(TEST_EMAIL2, TEST_NICKNAME2);
        final long postId = 게시글_작성_후_ID_추출(at1, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(postId, at1);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + at2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("댓글 수정/삭제 권한이 없습니다."));
    }

    @Test
    @DisplayName("4-3. 비회원 올바른 X-Guest-Password → 200, 이후 목록 조회 시 해당 댓글 미포함")
    void 댓글_삭제_비회원_올바른_비밀번호_성공() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .header("X-Guest-Password", GUEST_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get(POSTS_URL + "/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("4-4. 비회원 틀린 X-Guest-Password → 403, '비밀번호가 일치하지 않습니다.'")
    void 댓글_삭제_비회원_틀린_비밀번호_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId)
                        .header("X-Guest-Password", "wrongpassword"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("4-5. 비회원 X-Guest-Password 헤더 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
    void 댓글_삭제_비회원_헤더_없음_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 비회원_댓글_작성_후_ID_추출(postId, GUEST_PASSWORD);

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/" + commentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
    }

    @Test
    @DisplayName("4-6. 존재하지 않는 댓글 삭제 → 404, '댓글을 찾을 수 없습니다.'")
    void 댓글_삭제_없는_댓글_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long postId = 게시글_작성_후_ID_추출(at, "FREE");

        mockMvc.perform(delete(POSTS_URL + "/" + postId + "/comments/99999")
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("4-7. 다른 게시글의 댓글 ID로 접근 → 404, '댓글을 찾을 수 없습니다.'")
    void 댓글_삭제_다른_게시글_댓글_접근_실패() throws Exception {
        final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
        final long post1Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long post2Id = 게시글_작성_후_ID_추출(at, "FREE");
        final long commentId = 회원_댓글_작성_후_ID_추출(post1Id, at);

        mockMvc.perform(delete(POSTS_URL + "/" + post2Id + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("댓글을 찾을 수 없습니다."));
    }
}
