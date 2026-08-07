package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse;
import com.board.kimicetea.domain.post.dto.AdminPostSummaryResponse;
import com.board.kimicetea.domain.post.dto.AdminPostVisibilityRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.service.AdminPostService;
import com.board.kimicetea.domain.post.service.AdminPostStatsScheduler;
import com.board.kimicetea.domain.post.service.AdminPostStatsService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminPostController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: ROLE_ADMIN 인가는 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(AdminPostController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AdminPostService adminPostService;
    @MockBean private AdminPostStatsService adminPostStatsService;
    @MockBean private AdminPostStatsScheduler adminPostStatsScheduler;

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

    private static final String BASE_URL    = "/api/admin/posts";
    private static final String STATS_URL   = BASE_URL + "/stats";
    private static final String REFRESH_URL = BASE_URL + "/stats/refresh";
    private static final long   POST_ID     = 1L;

    @BeforeEach
    void setAdminAuth() {
        final CustomUserDetails admin = new CustomUserDetails(
                UUID.randomUUID(), "admin@example.com", null, "ADMIN", "device-admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    private AdminPostStatsResponse sampleStats() {
        return new AdminPostStatsResponse(
                1000L, 20L, 80L, 60L,
                List.of(new AdminPostStatsResponse.CountEntry("FREE", 600L),
                        new AdminPostStatsResponse.CountEntry("VOTE", 250L),
                        new AdminPostStatsResponse.CountEntry("DEBATE", 150L)),
                List.of(new AdminPostStatsResponse.CountEntry("INTJ", 120L)),
                List.of(new AdminPostStatsResponse.DailyCountEntry("07-01", 30L)));
    }

    private AdminPostSummaryResponse sampleSummary() {
        return new AdminPostSummaryResponse(
                POST_ID, "FREE", "테스트 게시글", "작성자", "INTJ", "TESTED",
                null, 10, 2, true, null, false, Instant.now());
    }

    private PostDetailResponse sampleDetail() {
        return new PostDetailResponse(
                POST_ID, "FREE", "제목", "내용", "작성자", "INTJ", "TESTED", null,
                5, 3, false, false, false, "ADMIN", false, Instant.now(), Instant.now(), null, null);
    }

    // ────────────────── GET /api/admin/posts/stats ──────────────────

    @Nested
    @DisplayName("GET /api/admin/posts/stats — 게시글 통계 조회")
    class GetPostStats {

        @Test
        @DisplayName("성공 — Redis 캐시에서 즉시 반환 → 200")
        void success() throws Exception {
            given(adminPostStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalPosts").value(1000))
                    .andExpect(jsonPath("$.data.hiddenCount").value(20))
                    .andExpect(jsonPath("$.data.thisMonthNewPosts").value(80))
                    .andExpect(jsonPath("$.data.lastMonthNewPosts").value(60));
        }

        @Test
        @DisplayName("성공 — byType·byMbti·postTrend 배열 포함 확인 → 200")
        void successCheckBreakdowns() throws Exception {
            given(adminPostStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.byType").isArray())
                    .andExpect(jsonPath("$.data.byType[0].name").value("FREE"))
                    .andExpect(jsonPath("$.data.byType[0].value").value(600))
                    .andExpect(jsonPath("$.data.byMbti").isArray())
                    .andExpect(jsonPath("$.data.postTrend").isArray())
                    .andExpect(jsonPath("$.data.postTrend[0].date").value("07-01"));
        }
    }

    // ────────────────── POST /api/admin/posts/stats/refresh ──────────────────

    @Nested
    @DisplayName("POST /api/admin/posts/stats/refresh — 게시글 통계 캐시 수동 갱신")
    class RefreshStats {

        @Test
        @DisplayName("성공 — triggerAsync() 호출 후 202 반환")
        void success() throws Exception {
            willDoNothing().given(adminPostStatsScheduler).triggerAsync();

            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── GET /api/admin/posts ──────────────────

    @Nested
    @DisplayName("GET /api/admin/posts — 게시글 목록 조회 (어드민)")
    class GetPosts {

        @Test
        @DisplayName("성공 — 필터 없이 기본 파라미터로 조회 → 200")
        void successWithDefaults() throws Exception {
            given(adminPostService.getPosts(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(POST_ID))
                    .andExpect(jsonPath("$.data.content[0].typeCode").value("FREE"));
        }

        @Test
        @DisplayName("성공 — typeCode=VOTE 필터 적용 → 200")
        void successWithTypeFilter() throws Exception {
            given(adminPostService.getPosts(eq("VOTE"), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE_URL).param("typeCode", "VOTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — isVisible=false(숨겨진 게시글) 필터 적용 → 200")
        void successWithVisibilityFilter() throws Exception {
            given(adminPostService.getPosts(any(), eq(false), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE_URL).param("isVisible", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("성공 — authorNickname 필터 적용 → 200")
        void successWithNicknameFilter() throws Exception {
            given(adminPostService.getPosts(any(), any(), eq("작성자"), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(BASE_URL).param("authorNickname", "작성자"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].authorNickname").value("작성자"));
        }

        @Test
        @DisplayName("성공 — sortDir=ASC 정렬 적용 → 200")
        void successWithAscSort() throws Exception {
            given(adminPostService.getPosts(any(), any(), any(), eq("ASC"), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(BASE_URL).param("sortDir", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — page 음수 전달 시 0으로 보정 → 200")
        void negativePageClampedToZero() throws Exception {
            given(adminPostService.getPosts(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE_URL).param("page", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — size 100 초과 전달 시 100으로 보정 → 200")
        void oversizedPageClampedToHundred() throws Exception {
            given(adminPostService.getPosts(any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(BASE_URL).param("size", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── GET /api/admin/posts/{id} ──────────────────

    @Nested
    @DisplayName("GET /api/admin/posts/{id} — 게시글 단건 조회 (어드민)")
    class GetPost {

        @Test
        @DisplayName("성공 — 숨겨진 게시글도 조회 가능 → 200")
        void success() throws Exception {
            given(adminPostService.getPost(POST_ID)).willReturn(sampleDetail());

            mockMvc.perform(get(BASE_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(POST_ID))
                    .andExpect(jsonPath("$.data.isVisible").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 ID → 404")
        void postNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                    .given(adminPostService).getPost(POST_ID);

            mockMvc.perform(get(BASE_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── DELETE /api/admin/posts/{id} ──────────────────

    @Nested
    @DisplayName("DELETE /api/admin/posts/{id} — 게시글 삭제 (어드민)")
    class DeletePost {

        @Test
        @DisplayName("성공 — 게시글 영구 삭제 → 200 및 삭제 메시지")
        void success() throws Exception {
            willDoNothing().given(adminPostService).deletePost(POST_ID);

            mockMvc.perform(delete(BASE_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다."));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 삭제 → 404")
        void postNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                    .given(adminPostService).deletePost(POST_ID);

            mockMvc.perform(delete(BASE_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── PATCH /api/admin/posts/{id}/visibility ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/posts/{id}/visibility — 게시글 가시성 변경")
    class ChangePostVisibility {

        private String visibilityUrl(final long id) {
            return BASE_URL + "/" + id + "/visibility";
        }

        @Test
        @DisplayName("성공 — visible=false(숨김) → 200 및 '숨겨졌습니다' 메시지")
        void successHide() throws Exception {
            willDoNothing().given(adminPostService).changePostVisibility(eq(POST_ID), eq(false));

            mockMvc.perform(patch(visibilityUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시글이 숨겨졌습니다."));
        }

        @Test
        @DisplayName("성공 — visible=true(복구) → 200 및 '복구되었습니다' 메시지")
        void successRestore() throws Exception {
            willDoNothing().given(adminPostService).changePostVisibility(eq(POST_ID), eq(true));

            mockMvc.perform(patch(visibilityUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시글이 복구되었습니다."));
        }

        @Test
        @DisplayName("실패 — visible 필드 누락(@NotNull 위반) → 400")
        void missingVisible() throws Exception {
            mockMvc.perform(patch(visibilityUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 가시성 변경 → 404")
        void postNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                    .given(adminPostService).changePostVisibility(eq(POST_ID), any(Boolean.class));

            mockMvc.perform(patch(visibilityUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminPostVisibilityRequest(false))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }
    }
}
