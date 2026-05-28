package com.board.kimicetea.domain.post;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.post.dto.AdminPostVisibilityRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.entity.Post;
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
 * 어드민 게시글 관리 API 통합 테스트 (7-2)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 */
@AutoConfigureMockMvc
class AdminPostControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired TokenRedisService tokenRedisService;
    @Autowired StringRedisTemplate redisTemplate;
    @MockBean  EmailService emailService;

    private static final String ADMIN_EMAIL    = "admin-post-test@example.com";
    private static final String ADMIN_PASSWORD = "password123!";
    private static final String ADMIN_NICKNAME = "어드민게시글테스터";

    private static final String USER_EMAIL    = "user-post-test@example.com";
    private static final String USER_PASSWORD = "password123!";
    private static final String USER_NICKNAME = "일반유저게시글테스터";

    private static final String TEST_MBTI = "INTJ";
    private static final String CONTENT   = "{\"type\":\"doc\",\"content\":[]}";

    @AfterEach
    void tearDown() {
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

    /** 어드민으로 게시글 숨김 처리 */
    private void 어드민_게시글_숨김(final String adminAt, final long postId) throws Exception {
        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/admin/posts
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-2-P-1. 어드민 게시글 전체 목록 조회 (숨김 포함) → 200 OK, 전체 반환")
    void 어드민_게시글_전체_목록_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);
        어드민_게시글_숨김(adminAt, postId);
        게시글_작성_후_ID_추출(userAt); // visible 게시글 1개 추가

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("7-2-P-2. isVisible=true 필터 → visible 게시글만 반환")
    void 어드민_게시글_visible_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long hiddenId = 게시글_작성_후_ID_추출(userAt);
        어드민_게시글_숨김(adminAt, hiddenId);
        게시글_작성_후_ID_추출(userAt); // visible 1개

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .param("isVisible", "true")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isVisible").value(true));
    }

    @Test
    @DisplayName("7-2-P-3. isVisible=false 필터 → 숨김 게시글만 반환")
    void 어드민_게시글_hidden_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long hiddenId = 게시글_작성_후_ID_추출(userAt);
        어드민_게시글_숨김(adminAt, hiddenId);
        게시글_작성_후_ID_추출(userAt); // visible 1개

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .param("isVisible", "false")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].isVisible").value(false));
    }

    @Test
    @DisplayName("7-2-P-4. typeCode=FREE 필터 → FREE 게시글만 반환")
    void 어드민_게시글_typeCode_필터_조회_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        게시글_작성_후_ID_추출(userAt);
        게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .param("typeCode", "FREE")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].typeCode").value("FREE"));
    }

    @Test
    @DisplayName("7-2-P-5. 존재하지 않는 typeCode 필터 → 빈 목록 반환")
    void 어드민_게시글_존재하지_않는_typeCode_필터_빈결과() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .param("typeCode", "UNKNOWN")
                .header("Authorization", "Bearer " + adminAt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("7-2-P-6. 비인가 유저(USER) 어드민 게시글 목록 조회 → 403")
    void 비인가_유저_게시글_목록_조회_실패() throws Exception {
        final String userAt = 유저_로그인_AT_추출();

        mockMvc.perform(get(ADMIN_POSTS_URL)
                .header("Authorization", "Bearer " + userAt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7-2-P-7. 미인증 게시글 목록 조회 → 401")
    void 미인증_게시글_목록_조회_실패() throws Exception {
        mockMvc.perform(get(ADMIN_POSTS_URL))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/admin/posts/{id}/visibility
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("7-2-P-8. 게시글 숨김 → 200 OK, isVisible=false 확인")
    void 어드민_게시글_숨김_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글이 숨겨졌습니다."));

        final Post post = postRepository.findById(postId).orElseThrow();
        assertThat(post.getIsVisible()).isFalse();
    }

    @Test
    @DisplayName("7-2-P-9. 숨김 게시글 복구 → 200 OK, isVisible=true 확인")
    void 어드민_게시글_복구_성공() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);
        어드민_게시글_숨김(adminAt, postId);

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글이 복구되었습니다."));

        final Post post = postRepository.findById(postId).orElseThrow();
        assertThat(post.getIsVisible()).isTrue();
    }

    @Test
    @DisplayName("7-2-P-10. visible 필드 누락 → 400")
    void visible_필드_누락_실패() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final String userAt  = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7-2-P-11. 존재하지 않는 게시글 숨김 → 404")
    void 존재하지_않는_게시글_숨김_실패() throws Exception {
        final String adminAt = 어드민_로그인_AT_추출();
        final long nonExistId = 999_999L;

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, nonExistId))
                .header("Authorization", "Bearer " + adminAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7-2-P-12. 비인가 유저(USER) 게시글 숨김 → 403")
    void 비인가_유저_게시글_숨김_실패() throws Exception {
        final String userAt = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .header("Authorization", "Bearer " + userAt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7-2-P-13. 미인증 게시글 숨김 → 401")
    void 미인증_게시글_숨김_실패() throws Exception {
        final String userAt = 유저_로그인_AT_추출();
        final long postId = 게시글_작성_후_ID_추출(userAt);

        mockMvc.perform(patch(String.format(ADMIN_POST_VISIBILITY_URL, postId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                .andExpect(status().isUnauthorized());
    }
}
