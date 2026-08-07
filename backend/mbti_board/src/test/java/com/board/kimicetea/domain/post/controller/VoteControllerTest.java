package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.dto.VoteOptionResponse;
import com.board.kimicetea.domain.post.dto.VoteRequest;
import com.board.kimicetea.domain.post.dto.VoteResultResponse;
import com.board.kimicetea.domain.post.service.VoteService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VoteController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: castVote는 회원 전용(@PreAuthorize) — 슬라이스에서는 Security 우회, 서비스 동작만 검증
 */
@WebMvcTest(VoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class VoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private VoteService voteService;

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

    private static final long POST_ID   = 10L;
    private static final long OPTION_ID = 100L;
    private UUID userId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUserAuth() {
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "user@example.com", null, "USER", "device-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private VoteResultResponse sampleResult() {
        final List<VoteOptionResponse> options = List.of(
                new VoteOptionResponse(OPTION_ID, "선택지 A", null, 10),
                new VoteOptionResponse(OPTION_ID + 1, "선택지 B", null, 5));
        return new VoteResultResponse(options, OPTION_ID, false, null);
    }

    private String voteUrl(final long postId) {
        return "/api/posts/" + postId + "/vote";
    }

    private String resultUrl(final long postId) {
        return "/api/posts/" + postId + "/vote/result";
    }

    // ────────────────── POST /api/posts/{postId}/vote ──────────────────

    @Nested
    @DisplayName("POST /api/posts/{postId}/vote — 투표 참여")
    class CastVote {

        @Test
        @DisplayName("성공 — 유효한 optionId로 투표 참여 → 200")
        void success() throws Exception {
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willReturn(sampleResult());

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(OPTION_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.options").isArray())
                    .andExpect(jsonPath("$.data.options.length()").value(2));
        }

        @Test
        @DisplayName("성공 — 이미 투표한 선택지 변경 → 200 및 myVotedOptionId 갱신")
        void successChangeVote() throws Exception {
            final VoteResultResponse changedResult = new VoteResultResponse(
                    sampleResult().options(), OPTION_ID + 1, false, null);
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willReturn(changedResult);

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(OPTION_ID + 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.myVotedOptionId").value(OPTION_ID + 1));
        }

        @Test
        @DisplayName("실패 — optionId null(@NotNull 위반) → 400")
        void nullOptionId() throws Exception {
            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 투표 게시글 → 404")
        void votePostNotFound() throws Exception {
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(OPTION_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.VOTE_POST_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 선택지 ID → 404")
        void voteOptionNotFound() throws Exception {
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.VOTE_OPTION_NOT_FOUND));

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(9999L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 선택지가 해당 게시글에 속하지 않음 → 400")
        void voteOptionNotBelongs() throws Exception {
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.VOTE_OPTION_NOT_BELONGS));

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(OPTION_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 마감된 투표에 참여 시도 → 400")
        void voteEnded() throws Exception {
            given(voteService.castVote(eq(POST_ID), any(VoteRequest.class), eq(userId)))
                    .willThrow(new CustomException(ErrorCode.VOTE_ENDED));

            mockMvc.perform(post(voteUrl(POST_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new VoteRequest(OPTION_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.VOTE_ENDED.getMessage()));
        }
    }

    // ────────────────── GET /api/posts/{postId}/vote/result ──────────────────

    @Nested
    @DisplayName("GET /api/posts/{postId}/vote/result — 투표 결과 조회")
    class GetVoteResult {

        @Test
        @DisplayName("성공 — 회원으로 투표 결과 조회 → 200 및 myVotedOptionId 포함")
        void successAsUser() throws Exception {
            given(voteService.getVoteResult(eq(POST_ID), eq(userId))).willReturn(sampleResult());

            mockMvc.perform(get(resultUrl(POST_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.options").isArray())
                    .andExpect(jsonPath("$.data.myVotedOptionId").value(OPTION_ID))
                    .andExpect(jsonPath("$.data.isEnded").value(false));
        }

        @Test
        @DisplayName("성공 — 비회원으로 투표 결과 조회(currentUserId=null) → 200 및 myVotedOptionId=null")
        void successAsGuest() throws Exception {
            SecurityContextHolder.clearContext();
            final VoteResultResponse guestResult = new VoteResultResponse(
                    sampleResult().options(), null, false, null);
            given(voteService.getVoteResult(eq(POST_ID), isNull())).willReturn(guestResult);

            mockMvc.perform(get(resultUrl(POST_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.myVotedOptionId").isEmpty());
        }

        @Test
        @DisplayName("성공 — 마감된 투표 결과 조회 → 200 및 isEnded=true")
        void successEnded() throws Exception {
            final VoteResultResponse endedResult = new VoteResultResponse(
                    sampleResult().options(), null, true, null);
            given(voteService.getVoteResult(eq(POST_ID), eq(userId))).willReturn(endedResult);

            mockMvc.perform(get(resultUrl(POST_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isEnded").value(true));
        }

        @Test
        @DisplayName("성공 — options 내 voteCount 포함 확인 → 200")
        void successCheckVoteCount() throws Exception {
            given(voteService.getVoteResult(eq(POST_ID), eq(userId))).willReturn(sampleResult());

            mockMvc.perform(get(resultUrl(POST_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.options[0].voteCount").value(10))
                    .andExpect(jsonPath("$.data.options[1].voteCount").value(5));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 투표 게시글 → 404")
        void votePostNotFound() throws Exception {
            given(voteService.getVoteResult(eq(POST_ID), any()))
                    .willThrow(new CustomException(ErrorCode.VOTE_POST_NOT_FOUND));

            mockMvc.perform(get(resultUrl(POST_ID)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.VOTE_POST_NOT_FOUND.getMessage()));
        }
    }
}
