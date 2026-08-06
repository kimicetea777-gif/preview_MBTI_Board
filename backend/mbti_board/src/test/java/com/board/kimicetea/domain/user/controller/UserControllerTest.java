package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiTestHistoryResponse;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.MyPostPreviewResponse;
import com.board.kimicetea.domain.post.service.PostService;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserActivityStatsResponse;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.dto.WithdrawalRequest;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * UserController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: /me 엔드포인트 인증은 SecurityContextHolder 직접 주입으로 처리
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private MbtiService mbtiService;
    @MockBean private PostService postService;
    @MockBean private CookieUtils cookieUtils;

    @MockBean private JwtProvider jwtProvider;
    @MockBean private TokenRedisService tokenRedisService;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String ME_URL               = "/api/users/me";
    private static final String ME_POSTS_URL         = "/api/users/me/posts";
    private static final String ME_POSTS_PREVIEW_URL = "/api/users/me/posts/preview";
    private static final String ME_STATS_URL         = "/api/users/me/stats";
    private static final String ME_WITHDRAWAL_URL    = "/api/users/me/withdrawal-verify";
    private static final String ME_MBTI_HISTORY_URL  = "/api/users/me/mbti/history";

    private UUID userId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUserAuth() {
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "user@example.com", null, "USER", "device-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private MyProfileResponse sampleMyProfile() {
        return new MyProfileResponse(
                userId, "user@example.com", "테스터", null,
                "INTJ", "TESTED", "USER", "NORMAL",
                Instant.now(), Instant.now());
    }

    private UserProfileResponse sampleUserProfile() {
        return new UserProfileResponse(userId, "타인닉", null, "ENFP", "SELF");
    }

    private UserActivityStatsResponse sampleActivityStats() {
        return UserActivityStatsResponse.of(10L, 5L, "INTJ", Instant.now());
    }

    // ────────────────── GET /api/users/me ──────────────────

    @Nested
    @DisplayName("GET /api/users/me — 내 프로필 조회")
    class GetMyProfile {

        @Test
        @DisplayName("성공 — 정상 인증 유저 프로필 반환 → 200")
        void success() throws Exception {
            given(userService.getMyProfile(userId)).willReturn(sampleMyProfile());

            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("user@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터"))
                    .andExpect(jsonPath("$.data.mbtiId").value("INTJ"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 유저 → 404")
        void userNotFound() throws Exception {
            given(userService.getMyProfile(userId))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get(ME_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── PATCH /api/users/me ──────────────────

    @Nested
    @DisplayName("PATCH /api/users/me — 내 프로필 수정")
    class UpdateMyProfile {

        @Test
        @DisplayName("성공 — 닉네임 변경 → 200")
        void successNicknameChange() throws Exception {
            given(userService.updateMyProfile(eq(userId), any(UpdateProfileRequest.class)))
                    .willReturn(sampleMyProfile());

            mockMvc.perform(patch(ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateProfileRequest("새닉네임", null, "ENFP"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — 모든 필드 null(변경 없음) → 200")
        void successAllNull() throws Exception {
            given(userService.updateMyProfile(eq(userId), any(UpdateProfileRequest.class)))
                    .willReturn(sampleMyProfile());

            mockMvc.perform(patch(ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 닉네임 1자(@Size 위반) → 400")
        void tooShortNickname() throws Exception {
            mockMvc.perform(patch(ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateProfileRequest("닉", null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 잘못된 MBTI 형식(@Pattern 위반) → 400")
        void invalidMbtiFormat() throws Exception {
            mockMvc.perform(patch(ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateProfileRequest(null, null, "INVALID"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 닉네임 중복 → 409")
        void duplicateNickname() throws Exception {
            given(userService.updateMyProfile(eq(userId), any(UpdateProfileRequest.class)))
                    .willThrow(new CustomException(ErrorCode.DUPLICATE_NICKNAME));

            mockMvc.perform(patch(ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateProfileRequest("중복닉네임", null, null))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_NICKNAME.getMessage()));
        }
    }

    // ────────────────── GET /api/users/{id} ──────────────────

    @Nested
    @DisplayName("GET /api/users/{id} — 타인 공개 프로필 조회")
    class GetUserProfile {

        @Test
        @DisplayName("성공 — 유효한 UUID로 공개 프로필 반환 → 200")
        void success() throws Exception {
            given(userService.getUserProfile(userId)).willReturn(sampleUserProfile());

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("타인닉"))
                    .andExpect(jsonPath("$.data.mbtiId").value("ENFP"))
                    .andExpect(jsonPath("$.data.mbtiVerified").value("SELF"));
        }

        @Test
        @DisplayName("성공 — 민감 정보(email·state) 미포함 확인 → 200")
        void successNoSensitiveData() throws Exception {
            given(userService.getUserProfile(userId)).willReturn(sampleUserProfile());

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.state").doesNotExist());
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 유저 UUID → 404")
        void userNotFound() throws Exception {
            given(userService.getUserProfile(userId))
                    .willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(get("/api/users/" + userId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── GET /api/users/me/posts ──────────────────

    @Nested
    @DisplayName("GET /api/users/me/posts — 내 게시글 목록 조회")
    class GetMyPosts {

        @Test
        @DisplayName("성공 — 기본 파라미터로 조회 → 200")
        void successWithDefaults() throws Exception {
            given(postService.getMyPosts(eq(userId), any(), any(PostSortType.class), any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            mockMvc.perform(get(ME_POSTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("성공 — type·sort 파라미터 적용 → 200")
        void successWithTypeAndSort() throws Exception {
            given(postService.getMyPosts(eq(userId), eq("FREE"), any(PostSortType.class), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(ME_POSTS_URL)
                            .param("type", "FREE")
                            .param("sort", "LATEST"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — size=0(@Min 위반) → 400")
        void sizeZero() throws Exception {
            mockMvc.perform(get(ME_POSTS_URL).param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — size=101(@Max 위반) → 400")
        void sizeExceedsMax() throws Exception {
            mockMvc.perform(get(ME_POSTS_URL).param("size", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── GET /api/users/me/posts/preview ──────────────────

    @Nested
    @DisplayName("GET /api/users/me/posts/preview — 내 게시글 프리뷰 조회")
    class GetMyPostsPreview {

        @Test
        @DisplayName("성공 — 최근 5개 프리뷰 반환 → 200")
        void success() throws Exception {
            final List<MyPostPreviewResponse> previews = List.of(
                    new MyPostPreviewResponse(1L, "FREE", "제목1", 3, 10, Instant.now()),
                    new MyPostPreviewResponse(2L, "VOTE", "제목2", 0, 5, Instant.now()));
            given(postService.getMyPostsPreview(userId)).willReturn(previews);

            mockMvc.perform(get(ME_POSTS_PREVIEW_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].title").value("제목1"))
                    .andExpect(jsonPath("$.data[1].typeCode").value("VOTE"));
        }

        @Test
        @DisplayName("성공 — 게시글 없는 경우 빈 배열 → 200")
        void successEmpty() throws Exception {
            given(postService.getMyPostsPreview(userId)).willReturn(List.of());

            mockMvc.perform(get(ME_POSTS_PREVIEW_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ────────────────── GET /api/users/me/stats ──────────────────

    @Nested
    @DisplayName("GET /api/users/me/stats — 내 활동 통계 조회")
    class GetMyActivityStats {

        @Test
        @DisplayName("성공 — 게시글·댓글·최근 MBTI 검사 통계 반환 → 200")
        void success() throws Exception {
            given(userService.getMyActivityStats(userId)).willReturn(sampleActivityStats());

            mockMvc.perform(get(ME_STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postCount").value(10))
                    .andExpect(jsonPath("$.data.commentCount").value(5))
                    .andExpect(jsonPath("$.data.latestTestMbtiId").value("INTJ"));
        }

        @Test
        @DisplayName("성공 — MBTI 검사 이력 없는 경우(latestTestMbtiId=null) → 200")
        void successNoMbtiHistory() throws Exception {
            given(userService.getMyActivityStats(userId))
                    .willReturn(UserActivityStatsResponse.of(0L, 0L, null, null));

            mockMvc.perform(get(ME_STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postCount").value(0))
                    .andExpect(jsonPath("$.data.latestTestMbtiId").isEmpty());
        }
    }

    // ────────────────── POST /api/users/me/withdrawal-verify ──────────────────

    @Nested
    @DisplayName("POST /api/users/me/withdrawal-verify — 탈퇴 OTP 발송")
    class SendWithdrawalOtp {

        @Test
        @DisplayName("성공 — OTP 발송 완료 → 200 및 안내 메시지")
        void success() throws Exception {
            willDoNothing().given(userService).sendWithdrawalOtp(userId);

            mockMvc.perform(post(ME_WITHDRAWAL_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 유저 → 404")
        void userNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.USER_NOT_FOUND))
                    .given(userService).sendWithdrawalOtp(userId);

            mockMvc.perform(post(ME_WITHDRAWAL_URL))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── DELETE /api/users/me ──────────────────

    @Nested
    @DisplayName("DELETE /api/users/me — 회원 탈퇴")
    class Withdraw {

        @Test
        @DisplayName("성공 — 유효한 OTP 코드로 탈퇴 처리 → 200")
        void success() throws Exception {
            willDoNothing().given(userService)
                    .withdraw(eq(userId), eq("test-access-token"), eq("123456"), any(HttpServletRequest.class));
            willDoNothing().given(cookieUtils).clearRefreshTokenCookie(any(HttpServletResponse.class));

            mockMvc.perform(delete(ME_URL)
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new WithdrawalRequest("123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("탈퇴 처리가 완료되었습니다."));
        }

        @Test
        @DisplayName("실패 — code 빈 문자열(@NotBlank 위반) → 400")
        void blankCode() throws Exception {
            mockMvc.perform(delete(ME_URL)
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new WithdrawalRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — OTP 코드 5자리(@Size min=6 위반) → 400")
        void codeTooShort() throws Exception {
            mockMvc.perform(delete(ME_URL)
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new WithdrawalRequest("12345"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — OTP 불일치 → 400")
        void invalidOtp() throws Exception {
            willThrow(new CustomException(ErrorCode.WITHDRAWAL_OTP_INVALID))
                    .given(userService).withdraw(eq(userId), any(), any(), any(HttpServletRequest.class));

            mockMvc.perform(delete(ME_URL)
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new WithdrawalRequest("000000"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.WITHDRAWAL_OTP_INVALID.getMessage()));
        }

        @Test
        @DisplayName("실패 — OTP 만료 → 400")
        void expiredOtp() throws Exception {
            willThrow(new CustomException(ErrorCode.WITHDRAWAL_OTP_EXPIRED))
                    .given(userService).withdraw(eq(userId), any(), any(), any(HttpServletRequest.class));

            mockMvc.perform(delete(ME_URL)
                            .header("Authorization", "Bearer test-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new WithdrawalRequest("123456"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.WITHDRAWAL_OTP_EXPIRED.getMessage()));
        }
    }

    // ────────────────── GET /api/users/me/mbti/history ──────────────────

    @Nested
    @DisplayName("GET /api/users/me/mbti/history — 내 MBTI 검사 이력 조회")
    class GetMyMbtiHistory {

        @Test
        @DisplayName("성공 — MBTI 검사 이력 목록 반환 → 200")
        void success() throws Exception {
            final List<MbtiTestHistoryResponse> history = List.of(
                    new MbtiTestHistoryResponse(1L, "INTJ", (short) 70, (short) 60, (short) 55, (short) 65, "v1", Instant.now()),
                    new MbtiTestHistoryResponse(2L, "ENFP", (short) 30, (short) 40, (short) 45, (short) 35, "v1", Instant.now()));
            given(mbtiService.getMyMbtiHistory(userId)).willReturn(history);

            mockMvc.perform(get(ME_MBTI_HISTORY_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].mbtiId").value("INTJ"));
        }

        @Test
        @DisplayName("성공 — 검사 이력 없는 경우 빈 배열 → 200")
        void successEmpty() throws Exception {
            given(mbtiService.getMyMbtiHistory(userId)).willReturn(List.of());

            mockMvc.perform(get(ME_MBTI_HISTORY_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

}
