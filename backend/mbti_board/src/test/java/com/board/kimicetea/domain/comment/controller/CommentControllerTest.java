package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.CommentCreateRequest;
import com.board.kimicetea.domain.comment.dto.CommentResponse;
import com.board.kimicetea.domain.comment.dto.CommentUpdateRequest;
import com.board.kimicetea.domain.comment.service.CommentService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.oauth2.RedisOAuth2AuthorizedClientService;
import com.board.kimicetea.global.security.session.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CommentController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 */
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean private TokenRedisService tokenRedisService;

    // 세션 아키텍처 마이그레이션(memo2.md) — SecurityConfig 의존성
    @MockBean
    private SessionService sessionService;

    @MockBean
    private AuthorizedClientRefreshService authorizedClientRefreshService;

    @MockBean
    private RedisOAuth2AuthorizedClientService redisOAuth2AuthorizedClientService;

    @MockBean
    private UserRepository userRepository;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final Long POST_ID = 1L;
    private static final Long COMMENT_ID = 10L;
    private static final String BASE_URL = "/api/posts/" + POST_ID + "/comments";
    private static final String COMMENT_URL = BASE_URL + "/" + COMMENT_ID;

    private UsernamePasswordAuthenticationToken memberAuth() {
        final CustomUserDetails details = new CustomUserDetails(
                UUID.randomUUID(), "test@example.com", null, "USER", "device-001");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private CommentResponse sampleResponse() {
        return new CommentResponse(COMMENT_ID, "테스트 댓글", "테스터", "INTJ", "SELF",
                0, 0, null, null, null, false, true,
                Instant.now(), Instant.now(), List.of());
    }

    // ────────────────── GET /api/posts/{postId}/comments ──────────────────

    @Nested
    @DisplayName("GET /api/posts/{postId}/comments — 댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("성공 — 비인증 요청 → 200 및 댓글 목록 반환")
        void successAsAnonymous() throws Exception {
            given(commentService.getComments(eq(POST_ID), isNull()))
                    .willReturn(List.of(sampleResponse()));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(COMMENT_ID));
        }

        @Test
        @DisplayName("성공 — 인증 요청 → 200 및 myReactionType 포함")
        void successAsAuthenticated() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(memberAuth());
            given(commentService.getComments(eq(POST_ID), any()))
                    .willReturn(List.of(sampleResponse()));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @AfterEach
        void clearAuth() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("실패 — 게시글 없음 → 404")
        void postNotFound() throws Exception {
            given(commentService.getComments(eq(POST_ID), any()))
                    .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── POST /api/posts/{postId}/comments ──────────────────

    @Nested
    @DisplayName("POST /api/posts/{postId}/comments — 댓글 작성")
    class CreateComment {

        @BeforeEach
        void setUp() {
            SecurityContextHolder.getContext().setAuthentication(memberAuth());
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("성공 — 회원이 댓글 작성 → 201")
        void successAsMember() throws Exception {
            given(commentService.createComment(eq(POST_ID), any(), any(), any()))
                    .willReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("테스트 댓글 내용", null, null, null, true, true, true))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(COMMENT_ID));
        }

        @Test
        @DisplayName("성공 — 비회원이 guestPassword 포함하여 작성 → 201")
        void successAsGuest() throws Exception {
            given(commentService.createComment(eq(POST_ID), any(), any(), any()))
                    .willReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("비회원 댓글 내용", null, "guest1234", null, true, true, true))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — parentId 포함 → 대댓글로 작성 201")
        void successAsReply() throws Exception {
            given(commentService.createComment(eq(POST_ID), any(), any(), any()))
                    .willReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("대댓글 내용", 5L, null, null, true, true, true))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — content 누락(@NotBlank 위반) → 400")
        void missingContent() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"parentId\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — content 1000자 초과(@Size 위반) → 400")
        void contentTooLong() throws Exception {
            final String longContent = "a".repeat(1001);
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest(longContent, null, null, null, true, true, true))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — guestPassword 4자 미만(@Size 위반) → 400")
        void guestPasswordTooShort() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("댓글 내용", null, "abc", null, true, true, true))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 게시글 없음 → 404")
        void postNotFound() throws Exception {
            given(commentService.createComment(eq(POST_ID), any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("댓글 내용", null, null, null, true, true, true))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 — 대댓글 깊이 초과(대댓글의 대댓글) → 400")
        void replyDepthExceeded() throws Exception {
            given(commentService.createComment(eq(POST_ID), any(), any(), any()))
                    .willThrow(new CustomException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentCreateRequest("대댓글 내용", 5L, null, null, true, true, true))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED.getMessage()));
        }
    }

    // ───────────────── PATCH /api/posts/{postId}/comments/{id} ─────────────────

    @Nested
    @DisplayName("PATCH /api/posts/{postId}/comments/{id} — 댓글 수정")
    class UpdateComment {

        @BeforeEach
        void setUp() {
            SecurityContextHolder.getContext().setAuthentication(memberAuth());
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("성공 — 유효한 요청 → 200 및 수정된 댓글 반환")
        void success() throws Exception {
            final CommentResponse updated = new CommentResponse(
                    COMMENT_ID, "수정된 내용", "테스터", "INTJ", "SELF",
                    0, 0, null, null, null, false, true,
                    Instant.now(), Instant.now(), List.of());
            given(commentService.updateComment(eq(POST_ID), eq(COMMENT_ID), any(), any()))
                    .willReturn(updated);

            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentUpdateRequest("수정된 내용", null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("수정된 내용"));
        }

        @Test
        @DisplayName("실패 — content 누락(@NotBlank 위반) → 400")
        void missingContent() throws Exception {
            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — content 1000자 초과(@Size 위반) → 400")
        void contentTooLong() throws Exception {
            final String longContent = "b".repeat(1001);
            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentUpdateRequest(longContent, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 다른 사용자의 댓글 수정 시도 → 403")
        void forbidden() throws Exception {
            given(commentService.updateComment(eq(POST_ID), eq(COMMENT_ID), any(), any()))
                    .willThrow(new CustomException(ErrorCode.COMMENT_FORBIDDEN));

            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentUpdateRequest("수정 내용", null))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("실패 — 비회원 비밀번호 불일치 → 403")
        void guestPasswordMismatch() throws Exception {
            given(commentService.updateComment(eq(POST_ID), eq(COMMENT_ID), any(), any()))
                    .willThrow(new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH));

            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentUpdateRequest("수정 내용", "wrongpw"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 댓글 없음 → 404")
        void commentNotFound() throws Exception {
            given(commentService.updateComment(eq(POST_ID), eq(COMMENT_ID), any(), any()))
                    .willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            mockMvc.perform(patch(COMMENT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CommentUpdateRequest("수정 내용", null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        }
    }

    // ───────────────── DELETE /api/posts/{postId}/comments/{id} ─────────────────

    @Nested
    @DisplayName("DELETE /api/posts/{postId}/comments/{id} — 댓글 삭제")
    class DeleteComment {

        @BeforeEach
        void setUp() {
            SecurityContextHolder.getContext().setAuthentication(memberAuth());
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("성공 — 회원이 본인 댓글 삭제 → 200")
        void successAsMember() throws Exception {
            willDoNothing().given(commentService).deleteComment(eq(POST_ID), eq(COMMENT_ID), isNull(), any());

            mockMvc.perform(delete(COMMENT_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — 비회원이 X-Guest-Password 헤더로 삭제 → 200")
        void successAsGuest() throws Exception {
            willDoNothing().given(commentService).deleteComment(eq(POST_ID), eq(COMMENT_ID), eq("guest1234"), any());

            mockMvc.perform(delete(COMMENT_URL)
                            .header("X-Guest-Password", "guest1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 비회원 비밀번호 불일치 → 403")
        void guestPasswordMismatch() throws Exception {
            willThrow(new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH))
                    .given(commentService).deleteComment(eq(POST_ID), eq(COMMENT_ID), any(), any());

            mockMvc.perform(delete(COMMENT_URL)
                            .header("X-Guest-Password", "wrongpw"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.GUEST_PASSWORD_MISMATCH.getMessage()));
        }

        @Test
        @DisplayName("실패 — 다른 사용자의 댓글 삭제 시도 → 403")
        void forbidden() throws Exception {
            willThrow(new CustomException(ErrorCode.COMMENT_FORBIDDEN))
                    .given(commentService).deleteComment(eq(POST_ID), eq(COMMENT_ID), any(), any());

            mockMvc.perform(delete(COMMENT_URL))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 댓글 없음 → 404")
        void commentNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentService).deleteComment(eq(POST_ID), eq(COMMENT_ID), any(), any());

            mockMvc.perform(delete(COMMENT_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        }
    }
}
