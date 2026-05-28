package com.board.kimicetea.domain.comment;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.comment.dto.AdminCommentVisibilityRequest;
import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 어드민 댓글 관리 API 통합 테스트 (7-2)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 */
@AutoConfigureMockMvc
class AdminCommentControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired StringRedisTemplate redisTemplate;
    @MockBean  EmailService emailService;

    private static final String ADMIN_EMAIL    = "admin-comment-test@example.com";
    private static final String ADMIN_PASSWORD = "password123!";
    private static final String ADMIN_NICKNAME = "어드민댓글테스터";

    private static final String USER_EMAIL    = "user-comment-test@example.com";
    private static final String USER_PASSWORD = "password123!";
    private static final String USER_NICKNAME = "일반유저댓글테스터";

    private static final String TEST_MBTI = "INTJ";
    private static final String CONTENT   = "{\"type\":\"doc\",\"content\":[]}";

    @AfterEach
    void tearDown() {
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

    /** 어드민 회원가입 → role 직접 ADMIN 변경 → 로그인 후 AT 반환 */
    private String 어드민_로그인_AT_추출() throws Exception {
        tokenRedisService.markEmailVerified(ADMIN_EMAIL);
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        final User admin = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        admin.changeRole(UserRole.ADMIN);
        userRepository.save(admin);

        final String body = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    /** 일반 유저 회원가입 → 로그인 후 AT 반환 */
    private String 유저_로그인_AT_추출() throws Exception {
        tokenRedisService.markEmailVerified(USER_EMAIL);
        mockMvc.perform(post(SIGNUP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest(USER_EMAIL, USER_PASSWORD, USER_NICKNAME, TEST_MBTI))))
                .andExpect(status().isCreated());

        final String body = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(USER_EMAIL, USER_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    /** 회원 게시글 작성 → 게시글 ID 반환 */
    private long 게시글_작성_후_ID_추출(final String accessToken) throws Exception {
        final String body = mockMvc.perform(post(POSTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(
                        new PostCreateRequest("테스트 제목", CONTENT, "FREE", null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    /** 회원 댓글 작성 → 댓글 ID 반환 */
    private long 댓글_작성_후_ID_추출(final long postId, final String accessToken) throws Exception {
        final String body = mockMvc.perform(post(POSTS_URL + "/" + postId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(
                        new CommentCreateRequest("테스트 댓글", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    /** 어드민으로 댓글 숨김 처리 */
    private void 어드민_댓글_숨김(final String adminAt, final long commentId) throws Exception {
        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/admin/comments
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-2-C-1. 어드민 댓글 전체 목록 조회 (숨김 포함) → 200 OK, 전체 반환")
    void 어드민_댓글_전체_목록_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);
        어드민_댓글_숨김(adminAt, commentId);
        댓글_작성_후_ID_추출(postId, userAt); // visible 댓글 1개 추가

        mockMvc.perform(get(ADMIN_COMMENTS_URL)
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("7-2-C-2. postId 필터 → 해당 게시글 댓글만 반환")
    void 어드민_댓글_postId_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId1 = 게시글_작성_후_ID_추출(userAt);
        final long postId2 = 게시글_작성_후_ID_추출(userAt);
        댓글_작성_후_ID_추출(postId1, userAt);
        댓글_작성_후_ID_추출(postId1, userAt);
        댓글_작성_후_ID_추출(postId2, userAt); // 다른 게시글 댓글

        mockMvc.perform(get(ADMIN_COMMENTS_URL)
                .param("postId", String.valueOf(postId1))
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].postId").value(postId1));
    }

    @Test
    @DisplayName("7-2-C-3. isVisible=false 필터 → 숨김 댓글만 반환")
    void 어드민_댓글_hidden_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);
        final long hiddenId = 댓글_작성_후_ID_추출(postId, userAt);
        어드민_댓글_숨김(adminAt, hiddenId);
        댓글_작성_후_ID_추출(postId, userAt); // visible 댓글

        mockMvc.perform(get(ADMIN_COMMENTS_URL)
                .param("isVisible", "false")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isVisible").value(false));
    }

    @Test
    @DisplayName("7-2-C-4. isVisible=true 필터 → visible 댓글만 반환")
    void 어드민_댓글_visible_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);
        final long hiddenId = 댓글_작성_후_ID_추출(postId, userAt);
        어드민_댓글_숨김(adminAt, hiddenId);
        댓글_작성_후_ID_추출(postId, userAt); // visible 댓글

        mockMvc.perform(get(ADMIN_COMMENTS_URL)
                .param("isVisible", "true")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isVisible").value(true));
    }

    @Test
    @DisplayName("7-2-C-5. 비인가 유저(USER) 어드민 댓글 목록 조회 → 403")
    void 비인가_유저_댓글_목록_조회_실패() throws Exception {
        final String userAt = 유저_로그인_AT_추출();

        mockMvc.perform(get(ADMIN_COMMENTS_URL)
                .header("Authorization", "Bearer " + userAt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7-2-C-6. 미인증 댓글 목록 조회 → 401")
    void 미인증_댓글_목록_조회_실패() throws Exception {
        mockMvc.perform(get(ADMIN_COMMENTS_URL))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/admin/comments/{id}/visibility
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-2-C-7. 댓글 숨김 → 200 OK, isVisible=false 확인")
    void 어드민_댓글_숨김_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId    = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 숨겨졌습니다."));

        final Comment comment = commentRepository.findById(commentId).orElseThrow();
        assertThat(comment.getIsVisible()).isFalse();
    }

    @Test
    @DisplayName("7-2-C-8. 숨김 댓글 복구 → 200 OK, isVisible=true 확인")
    void 어드민_댓글_복구_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId    = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);
        어드민_댓글_숨김(adminAt, commentId);

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 복구되었습니다."));

        final Comment comment = commentRepository.findById(commentId).orElseThrow();
        assertThat(comment.getIsVisible()).isTrue();
    }

    @Test
    @DisplayName("7-2-C-9. visible 필드 누락 → 400")
    void visible_필드_누락_실패() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId    = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7-2-C-10. 존재하지 않는 댓글 숨김 → 404")
    void 존재하지_않는_댓글_숨김_실패() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final long nonExistId = 999_999L;

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, nonExistId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7-2-C-11. 비인가 유저(USER) 댓글 숨김 → 403")
    void 비인가_유저_댓글_숨김_실패() throws Exception {
        final String userAt  = 유저_로그인_AT_추출();
        final long postId    = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .header("Authorization", "Bearer " + userAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7-2-C-12. 미인증 댓글 숨김 → 401")
    void 미인증_댓글_숨김_실패() throws Exception {
        final String userAt  = 유저_로그인_AT_추출();
        final long postId    = 게시글_작성_후_ID_추출(userAt);
        final long commentId = 댓글_작성_후_ID_추출(postId, userAt);

        mockMvc.perform(patch(String.format(ADMIN_COMMENT_VISIBILITY_URL, commentId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                .andExpect(status().isUnauthorized());
    }
}
