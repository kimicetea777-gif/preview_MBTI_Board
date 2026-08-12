package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.CommentReactionRequest;
import com.board.kimicetea.domain.comment.dto.CommentReactionResponse;
import com.board.kimicetea.domain.comment.service.CommentReactionService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CommentReactionController 슬라이스 테스트 — 회원 전용 리액션 API
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: reactionType 1/-1 범위 검증은 서비스 레이어 담당 — 컨트롤러는 @NotNull만 검증
 */
@WebMvcTest(CommentReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CommentReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentReactionService reactionService;


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

    private static final Long COMMENT_ID = 10L;
    private static final String REACTIONS_URL = "/api/comments/" + COMMENT_ID + "/reactions";

    private UUID userId;

    @BeforeEach
    void setMemberAuth() {
        userId = UUID.randomUUID();
        final CustomUserDetails details = new CustomUserDetails(
                userId, "user@example.com", null, "USER", "device-001");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private CommentReactionResponse sampleResponse(final Short reactionType) {
        return new CommentReactionResponse(COMMENT_ID, reactionType, 5, 2);
    }

    // ────────────────── POST /api/comments/{commentId}/reactions ──────────────────

    @Nested
    @DisplayName("POST /api/comments/{commentId}/reactions — 리액션 토글")
    class ToggleReaction {

        @Test
        @DisplayName("성공 — reactionType=1(좋아요) 토글 → 200 및 최신 카운트 반환")
        void successLike() throws Exception {
            given(reactionService.toggleReaction(eq(COMMENT_ID), eq((short) 1), any()))
                    .willReturn(sampleResponse((short) 1));

            mockMvc.perform(post(REACTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data.myReactionType").value(1))
                    .andExpect(jsonPath("$.data.likeCount").value(5))
                    .andExpect(jsonPath("$.data.dislikeCount").value(2));
        }

        @Test
        @DisplayName("성공 — reactionType=-1(싫어요) 토글 → 200")
        void successDislike() throws Exception {
            given(reactionService.toggleReaction(eq(COMMENT_ID), eq((short) -1), any()))
                    .willReturn(sampleResponse((short) -1));

            mockMvc.perform(post(REACTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) -1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.myReactionType").value(-1));
        }

        @Test
        @DisplayName("성공 — 같은 타입 재토글 시 리액션 취소 → myReactionType null 반환")
        void successCancelOnSameType() throws Exception {
            given(reactionService.toggleReaction(eq(COMMENT_ID), eq((short) 1), any()))
                    .willReturn(sampleResponse(null));

            mockMvc.perform(post(REACTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.myReactionType").doesNotExist());
        }

        @Test
        @DisplayName("실패 — reactionType 누락(@NotNull 위반) → 400")
        void missingReactionType() throws Exception {
            mockMvc.perform(post(REACTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 댓글 없음 → 404")
        void commentNotFound() throws Exception {
            given(reactionService.toggleReaction(eq(COMMENT_ID), any(), any()))
                    .willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            mockMvc.perform(post(REACTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentReactionRequest((short) 1))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── DELETE /api/comments/{commentId}/reactions ──────────────────

    @Nested
    @DisplayName("DELETE /api/comments/{commentId}/reactions — 리액션 취소")
    class CancelReaction {

        @Test
        @DisplayName("성공 — 리액션 보유 시 삭제 → 200 및 myReactionType null 반환")
        void successWithExistingReaction() throws Exception {
            given(reactionService.cancelReaction(eq(COMMENT_ID), any()))
                    .willReturn(sampleResponse(null));

            mockMvc.perform(delete(REACTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.commentId").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data.myReactionType").doesNotExist());
        }

        @Test
        @DisplayName("성공 — 리액션 없어도 멱등하게 200 반환")
        void successIdempotentWithNoReaction() throws Exception {
            given(reactionService.cancelReaction(eq(COMMENT_ID), any()))
                    .willReturn(sampleResponse(null));

            mockMvc.perform(delete(REACTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 댓글 없음 → 404")
        void commentNotFound() throws Exception {
            given(reactionService.cancelReaction(eq(COMMENT_ID), any()))
                    .willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            mockMvc.perform(delete(REACTIONS_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        }
    }
}
