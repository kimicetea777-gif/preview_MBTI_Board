package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.AdminCommentStatsResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentSummaryResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentVisibilityRequest;
import com.board.kimicetea.domain.comment.service.AdminCommentService;
import com.board.kimicetea.domain.comment.service.AdminCommentStatsScheduler;
import com.board.kimicetea.domain.comment.service.AdminCommentStatsService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminCommentController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: ROLE_ADMIN 인가는 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(AdminCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AdminCommentService adminCommentService;
    @MockBean private AdminCommentStatsService adminCommentStatsService;
    @MockBean private AdminCommentStatsScheduler adminCommentStatsScheduler;


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

    private static final String STATS_URL   = "/api/admin/comments/stats";
    private static final String REFRESH_URL = "/api/admin/comments/stats/refresh";
    private static final String LIST_URL    = "/api/admin/comments";
    private static final long   COMMENT_ID  = 10L;

    @BeforeEach
    void setAdminAuth() {
        final CustomUserDetails admin = new CustomUserDetails(
                UUID.randomUUID(), "admin@example.com", null, "ADMIN", "device-admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    private AdminCommentStatsResponse sampleStats() {
        return new AdminCommentStatsResponse(
                100L, 5L, 20L, 15L,
                List.of(new AdminCommentStatsResponse.CountEntry("INTJ", 20L)),
                List.of(new AdminCommentStatsResponse.DailyCountEntry("07-01", 10L)));
    }

    private AdminCommentSummaryResponse sampleSummary() {
        return new AdminCommentSummaryResponse(
                COMMENT_ID, 1L, "FREE", null, "댓글 내용", "테스터",
                "INTJ", "SELF", 3, 1, false, true, Instant.now());
    }

    // ────────────────── GET /api/admin/comments/stats ──────────────────

    @Nested
    @DisplayName("GET /api/admin/comments/stats — 댓글 통계 조회")
    class GetCommentStats {

        @Test
        @DisplayName("성공 — Redis 캐시에서 즉시 반환 → 200")
        void success() throws Exception {
            given(adminCommentStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalComments").value(100))
                    .andExpect(jsonPath("$.data.hiddenCount").value(5));
        }

        @Test
        @DisplayName("성공 — byMbti 및 commentTrend 포함 확인 → 200")
        void successWithBreakdowns() throws Exception {
            given(adminCommentStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.byMbti").isArray())
                    .andExpect(jsonPath("$.data.byMbti[0].name").value("INTJ"))
                    .andExpect(jsonPath("$.data.commentTrend").isArray())
                    .andExpect(jsonPath("$.data.commentTrend[0].date").value("07-01"));
        }
    }

    // ────────────────── POST /api/admin/comments/stats/refresh ──────────────────

    @Nested
    @DisplayName("POST /api/admin/comments/stats/refresh — 통계 캐시 수동 갱신")
    class RefreshStats {

        @Test
        @DisplayName("성공 — triggerAsync() 호출 후 202 반환")
        void success() throws Exception {
            willDoNothing().given(adminCommentStatsScheduler).triggerAsync();

            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── GET /api/admin/comments ──────────────────

    @Nested
    @DisplayName("GET /api/admin/comments — 댓글 목록 조회 (어드민)")
    class GetComments {

        @Test
        @DisplayName("성공 — 필터 없이 기본 파라미터로 조회 → 200")
        void successWithDefaults() throws Exception {
            given(adminCommentService.getComments(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(LIST_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(COMMENT_ID));
        }

        @Test
        @DisplayName("성공 — postId 필터 적용 → 200")
        void successWithPostIdFilter() throws Exception {
            given(adminCommentService.getComments(eq(1L), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(LIST_URL).param("postId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — isVisible=false 필터(숨겨진 댓글) → 200")
        void successWithVisibleFilter() throws Exception {
            given(adminCommentService.getComments(any(), eq(false), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(LIST_URL).param("isVisible", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("성공 — authorNickname 필터 적용 → 200")
        void successWithNicknameFilter() throws Exception {
            given(adminCommentService.getComments(any(), any(), eq("테스터"), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(LIST_URL).param("authorNickname", "테스터"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — page 음수 전달 시 0으로 보정 → 200")
        void negativePageClampedToZero() throws Exception {
            given(adminCommentService.getComments(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(LIST_URL).param("page", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — size 100 초과 전달 시 100으로 보정 → 200")
        void oversizedPageClampedToHundred() throws Exception {
            given(adminCommentService.getComments(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(LIST_URL).param("size", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── PATCH /api/admin/comments/{id}/visibility ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/comments/{id}/visibility — 댓글 가시성 변경")
    class ChangeCommentVisibility {

        private String visibilityUrl(final long id) {
            return "/api/admin/comments/" + id + "/visibility";
        }

        @Test
        @DisplayName("성공 — visible=false(숨김) → 200 및 숨겨졌습니다 메시지")
        void successHide() throws Exception {
            willDoNothing().given(adminCommentService).changeCommentVisibility(eq(COMMENT_ID), eq(false));

            mockMvc.perform(patch(visibilityUrl(COMMENT_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("댓글이 숨겨졌습니다."));
        }

        @Test
        @DisplayName("성공 — visible=true(복구) → 200 및 복구되었습니다 메시지")
        void successRestore() throws Exception {
            willDoNothing().given(adminCommentService).changeCommentVisibility(eq(COMMENT_ID), eq(true));

            mockMvc.perform(patch(visibilityUrl(COMMENT_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("댓글이 복구되었습니다."));
        }

        @Test
        @DisplayName("실패 — visible 필드 누락(@NotNull 위반) → 400")
        void missingVisible() throws Exception {
            mockMvc.perform(patch(visibilityUrl(COMMENT_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 댓글 없음 → 404")
        void commentNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(adminCommentService).changeCommentVisibility(eq(COMMENT_ID), any(Boolean.class));

            mockMvc.perform(patch(visibilityUrl(COMMENT_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminCommentVisibilityRequest(false))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
        }
    }
}
