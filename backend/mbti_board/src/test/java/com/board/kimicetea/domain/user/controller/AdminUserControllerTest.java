package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.user.dto.AdminUpdateUserRequest;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse;
import com.board.kimicetea.domain.user.dto.AdminUserSummaryResponse;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.domain.user.service.AdminUserService;
import com.board.kimicetea.domain.user.service.AdminUserStatsScheduler;
import com.board.kimicetea.domain.user.service.AdminUserStatsService;
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
 * AdminUserController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: ROLE_ADMIN 인가는 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AdminUserService adminUserService;
    @MockBean private AdminUserStatsService adminUserStatsService;
    @MockBean private AdminUserStatsScheduler adminUserStatsScheduler;


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

    private static final String STATS_URL   = "/api/admin/users/stats";
    private static final String REFRESH_URL = "/api/admin/users/stats/refresh";
    private static final String USERS_URL   = "/api/admin/users";

    private UUID adminId;
    private CustomUserDetails adminDetails;

    @BeforeEach
    void setAdminAuth() {
        adminId = UUID.randomUUID();
        adminDetails = new CustomUserDetails(adminId, "admin@example.com", null, "ADMIN", "device-admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities()));
    }

    private AdminUserStatsResponse sampleStats() {
        return new AdminUserStatsResponse(
                500L, 20L, 15L, 30L, 25L, 5L,
                List.of(new AdminUserStatsResponse.CountEntry("NORMAL", 450L)),
                List.of(new AdminUserStatsResponse.CountEntry("USER", 480L)),
                List.of(new AdminUserStatsResponse.CountEntry("INTJ", 50L)),
                List.of(new AdminUserStatsResponse.CountEntry("TESTED", 120L)),
                List.of(new AdminUserStatsResponse.DailyCountEntry("07-01", 10L)),
                List.of(new AdminUserStatsResponse.DailyCountEntry("07-01", 2L)),
                List.of(new AdminUserStatsResponse.RoleTrendEntry("07-01", 1L, 0L)));
    }

    private AdminUserSummaryResponse sampleSummary(final UUID id) {
        return new AdminUserSummaryResponse(
                id, UUID.randomUUID(), "user@test.com", "테스터", null,
                "USER", "NORMAL", "INTJ", "TESTED",
                Instant.now(), Instant.now(), Instant.now());
    }

    // ────────────────── GET /api/admin/users/stats ──────────────────

    @Nested
    @DisplayName("GET /api/admin/users/stats — 유저 통계 조회")
    class GetUserStats {

        @Test
        @DisplayName("성공 — Redis 캐시에서 즉시 반환 → 200")
        void success() throws Exception {
            given(adminUserStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalUsers").value(500))
                    .andExpect(jsonPath("$.data.thisMonthNewUsers").value(30));
        }

        @Test
        @DisplayName("성공 — byState·byRole·byMbti 분포 배열 포함 확인 → 200")
        void successWithBreakdowns() throws Exception {
            given(adminUserStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.byState").isArray())
                    .andExpect(jsonPath("$.data.byState[0].name").value("NORMAL"))
                    .andExpect(jsonPath("$.data.byState[0].value").value(450))
                    .andExpect(jsonPath("$.data.byRole").isArray())
                    .andExpect(jsonPath("$.data.byMbti").isArray());
        }

        @Test
        @DisplayName("성공 — signupTrend·withdrawalTrend·roleTrend 포함 확인 → 200")
        void successWithTrends() throws Exception {
            given(adminUserStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.signupTrend").isArray())
                    .andExpect(jsonPath("$.data.signupTrend[0].date").value("07-01"))
                    .andExpect(jsonPath("$.data.withdrawalTrend").isArray())
                    .andExpect(jsonPath("$.data.roleTrend").isArray());
        }
    }

    // ────────────────── POST /api/admin/users/stats/refresh ──────────────────

    @Nested
    @DisplayName("POST /api/admin/users/stats/refresh — 통계 캐시 수동 갱신")
    class RefreshStats {

        @Test
        @DisplayName("성공 — triggerAsync() 호출 후 202 반환")
        void success() throws Exception {
            willDoNothing().given(adminUserStatsScheduler).triggerAsync();

            mockMvc.perform(post(REFRESH_URL))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── GET /api/admin/users ──────────────────

    @Nested
    @DisplayName("GET /api/admin/users — 유저 목록 조회")
    class GetUsers {

        @Test
        @DisplayName("성공 — 기본 파라미터로 조회 → 200")
        void successWithDefaults() throws Exception {
            final UUID targetId = UUID.randomUUID();
            given(adminUserService.getUsers(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary(targetId)), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.content[0].role").value("USER"));
        }

        @Test
        @DisplayName("성공 — 이메일 필터 적용 → 200")
        void successWithEmailFilter() throws Exception {
            given(adminUserService.getUsers(eq("filter@test.com"), any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL).param("email", "filter@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("성공 — state=LOCKED 필터 적용 → 200")
        void successWithStateFilter() throws Exception {
            given(adminUserService.getUsers(any(), eq(UserState.LOCKED), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL).param("state", "LOCKED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — role=ADMIN 필터 적용 → 200")
        void successWithRoleFilter() throws Exception {
            given(adminUserService.getUsers(any(), any(), eq(UserRole.ADMIN), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL).param("role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — page 음수 전달 시 0으로 보정 → 200")
        void negativePageClampedToZero() throws Exception {
            given(adminUserService.getUsers(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL).param("page", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — size 100 초과 시 100으로 보정 → 200")
        void oversizedPageClampedToHundred() throws Exception {
            given(adminUserService.getUsers(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL).param("size", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — 결과 없는 경우 빈 페이지 → 200")
        void successEmptyResult() throws Exception {
            given(adminUserService.getUsers(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(USERS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // ────────────────── PATCH /api/admin/users/{id} ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/users/{id} — 유저 통합 수정")
    class UpdateUser {

        private String userUrl(final UUID id) {
            return USERS_URL + "/" + id;
        }

        @Test
        @DisplayName("성공 — 닉네임·역할·상태 수정 → 200")
        void success() throws Exception {
            final UUID targetId = UUID.randomUUID();
            willDoNothing().given(adminUserService)
                    .updateUser(eq(adminId), eq(targetId), any(AdminUpdateUserRequest.class));

            mockMvc.perform(patch(userUrl(targetId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminUpdateUserRequest("새닉네임", null, UserRole.TESTER, UserState.NORMAL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("유저 정보가 수정되었습니다."));
        }

        @Test
        @DisplayName("성공 — clearProfileImage=true 로 프로필 사진 제거 → 200")
        void successClearProfileImage() throws Exception {
            final UUID targetId = UUID.randomUUID();
            willDoNothing().given(adminUserService)
                    .updateUser(eq(adminId), eq(targetId), any(AdminUpdateUserRequest.class));

            mockMvc.perform(patch(userUrl(targetId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminUpdateUserRequest(null, true, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — 모든 필드 null(변경 없음) → 200")
        void successAllNull() throws Exception {
            final UUID targetId = UUID.randomUUID();
            willDoNothing().given(adminUserService)
                    .updateUser(eq(adminId), eq(targetId), any(AdminUpdateUserRequest.class));

            mockMvc.perform(patch(userUrl(targetId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 닉네임 1자(@Size 위반) → 400")
        void tooShortNickname() throws Exception {
            final UUID targetId = UUID.randomUUID();

            mockMvc.perform(patch(userUrl(targetId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminUpdateUserRequest("닉", null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 자기 자신 수정 시도 → 400")
        void cannotModifySelf() throws Exception {
            willThrow(new CustomException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF))
                    .given(adminUserService).updateUser(eq(adminId), eq(adminId), any(AdminUpdateUserRequest.class));

            mockMvc.perform(patch(userUrl(adminId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminUpdateUserRequest(null, null, UserRole.USER, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.ADMIN_CANNOT_MODIFY_SELF.getMessage()));
        }

        @Test
        @DisplayName("실패 — 대상 유저 없음 → 404")
        void userNotFound() throws Exception {
            final UUID targetId = UUID.randomUUID();
            willThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
                    .given(adminUserService).updateUser(eq(adminId), eq(targetId), any(AdminUpdateUserRequest.class));

            mockMvc.perform(patch(userUrl(targetId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdminUpdateUserRequest(null, null, null, UserState.LOCKED))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }
}
