package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.DebatePostCreateRequest;
import com.board.kimicetea.domain.post.dto.DebateStanceRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.dto.VotePostCreateRequest;
import com.board.kimicetea.domain.post.service.PostService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.ApiConstants;
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
 * PostController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: 인증/권한 검증은 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private PostService postService;


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

    private static final String POSTS_URL  = "/api/posts";
    private static final long   POST_ID    = 1L;
    private UUID userId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUserAuth() {
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "user@example.com", null, "USER", "device-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private PostDetailResponse sampleDetail() {
        return new PostDetailResponse(
                POST_ID, "FREE", "제목", "내용", "작성자", "INTJ", "TESTED", null,
                5, 3, true, false, true, null, false, Instant.now(), Instant.now(), null, null);
    }

    private PostSummaryResponse sampleSummary() {
        return new PostSummaryResponse(
                POST_ID, "FREE", "제목", "내용 미리보기", null,
                "작성자", "INTJ", "TESTED", null, 5, 3, false, Instant.now(), null, null);
    }

    // ────────────────── GET /api/posts ──────────────────

    @Nested
    @DisplayName("GET /api/posts — 게시글 목록 조회")
    class GetPosts {

        @Test
        @DisplayName("성공 — 기본 파라미터로 조회 → 200")
        void successWithDefaults() throws Exception {
            given(postService.getPosts(isNull(), eq(PostSortType.LATEST), isNull(), eq("ALL"), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get(POSTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(POST_ID));
        }

        @Test
        @DisplayName("성공 — type=FREE 필터 적용 → 200")
        void successWithTypeFilter() throws Exception {
            given(postService.getPosts(eq("FREE"), any(), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(POSTS_URL).param("type", "FREE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — sort=VIEWS 정렬 적용 → 200")
        void successWithSortFilter() throws Exception {
            given(postService.getPosts(any(), eq(PostSortType.VIEWS), any(), any(), any()))
                    .willReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get(POSTS_URL).param("sort", "VIEWS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — authorMbti=INTJ 필터 적용 → 200")
        void successWithMbtiFilter() throws Exception {
            given(postService.getPosts(any(), any(), eq("INTJ"), any(), any()))
                    .willReturn(new PageImpl<>(List.of(sampleSummary())));

            mockMvc.perform(get(POSTS_URL).param("authorMbti", "INTJ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — page 음수(@Min(0) 위반) → 400")
        void negativePageFails() throws Exception {
            mockMvc.perform(get(POSTS_URL).param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — size 100 초과(@Max(100) 위반) → 400")
        void oversizedPageFails() throws Exception {
            mockMvc.perform(get(POSTS_URL).param("size", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── GET /api/posts/{id} ──────────────────

    @Nested
    @DisplayName("GET /api/posts/{id} — 게시글 단건 조회")
    class GetPost {

        @Test
        @DisplayName("성공 — 회원으로 게시글 조회 → 200")
        void successAsUser() throws Exception {
            given(postService.getPost(eq(POST_ID), eq(userId))).willReturn(sampleDetail());

            mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(POST_ID))
                    .andExpect(jsonPath("$.data.typeCode").value("FREE"))
                    .andExpect(jsonPath("$.data.title").value("제목"));
        }

        @Test
        @DisplayName("성공 — 비회원으로 게시글 조회(currentUserId=null) → 200")
        void successAsGuest() throws Exception {
            SecurityContextHolder.clearContext();
            given(postService.getPost(eq(POST_ID), isNull())).willReturn(sampleDetail());

            mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 ID → 404")
        void postNotFound() throws Exception {
            given(postService.getPost(eq(POST_ID), any()))
                    .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

            mockMvc.perform(get(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── POST /api/posts ──────────────────

    @Nested
    @DisplayName("POST /api/posts — 자유글 작성")
    class CreatePost {

        @Test
        @DisplayName("성공 — 회원이 자유글 작성 → 201")
        void successAsUser() throws Exception {
            given(postService.createPost(any(PostCreateRequest.class), any(), any()))
                    .willReturn(sampleDetail());

            final PostCreateRequest request = new PostCreateRequest("제목", "내용", "FREE", null, null, true, true, true);

            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(POST_ID));
        }

        @Test
        @DisplayName("성공 — 비회원이 guestPassword 포함 자유글 작성 → 201")
        void successAsGuest() throws Exception {
            given(postService.createPost(any(PostCreateRequest.class), isNull(), any()))
                    .willReturn(sampleDetail());

            final PostCreateRequest request = new PostCreateRequest("제목", "내용", "FREE", "pass1234", null, true, true, true);

            SecurityContextHolder.clearContext();
            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final PostCreateRequest request = new PostCreateRequest("", "내용", "FREE", null, null, true, true, true);

            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — content 빈 문자열(@NotBlank 위반) → 400")
        void blankContent() throws Exception {
            final PostCreateRequest request = new PostCreateRequest("제목", "", "FREE", null, null, true, true, true);

            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — guestPassword 3자(@Size min=4 위반) → 400")
        void tooShortGuestPassword() throws Exception {
            final PostCreateRequest request = new PostCreateRequest("제목", "내용", "FREE", "abc", null, true, true, true);

            mockMvc.perform(post(POSTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── POST /api/posts/debate ──────────────────

    @Nested
    @DisplayName("POST /api/posts/debate — 토론 게시글 작성")
    class CreateDebatePost {

        @Test
        @DisplayName("성공 — 유효한 토론 게시글 작성 → 201")
        void success() throws Exception {
            given(postService.createDebatePost(any(DebatePostCreateRequest.class), any(), any()))
                    .willReturn(sampleDetail());

            final DebatePostCreateRequest request = new DebatePostCreateRequest(
                    "토론 제목", "토론 내용", null, null, "찬성", "반대");

            mockMvc.perform(post(POSTS_URL + "/debate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — proLabel/conLabel null(기본값 사용) → 201")
        void successWithNullLabels() throws Exception {
            given(postService.createDebatePost(any(DebatePostCreateRequest.class), any(), any()))
                    .willReturn(sampleDetail());

            final DebatePostCreateRequest request = new DebatePostCreateRequest(
                    "토론 제목", "토론 내용", null, null, null, null);

            mockMvc.perform(post(POSTS_URL + "/debate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final DebatePostCreateRequest request = new DebatePostCreateRequest(
                    "", "내용", null, null, null, null);

            mockMvc.perform(post(POSTS_URL + "/debate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — proLabel 50자 초과(@Size 위반) → 400")
        void proLabelTooLong() throws Exception {
            final DebatePostCreateRequest request = new DebatePostCreateRequest(
                    "제목", "내용", null, null, "a".repeat(51), "반대");

            mockMvc.perform(post(POSTS_URL + "/debate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── POST /api/posts/vote ──────────────────

    @Nested
    @DisplayName("POST /api/posts/vote — 투표 게시글 작성")
    class CreateVotePost {

        private List<VoteOptionRequest> twoOptions() {
            return List.of(new VoteOptionRequest("선택지A", null), new VoteOptionRequest("선택지B", null));
        }

        @Test
        @DisplayName("성공 — 선택지 2개 유효한 투표 게시글 작성 → 201")
        void success() throws Exception {
            given(postService.createVotePost(any(VotePostCreateRequest.class), any(), any()))
                    .willReturn(sampleDetail());

            final VotePostCreateRequest request = new VotePostCreateRequest(
                    "투표 제목", "내용", null, twoOptions(), null);

            mockMvc.perform(post(POSTS_URL + "/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 선택지 1개(@Size min=2 위반) → 400")
        void tooFewOptions() throws Exception {
            final VotePostCreateRequest request = new VotePostCreateRequest(
                    "제목", "내용", null,
                    List.of(new VoteOptionRequest("선택지A", null)), null);

            mockMvc.perform(post(POSTS_URL + "/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 선택지 17개(@Size max=16 위반) → 400")
        void tooManyOptions() throws Exception {
            final List<VoteOptionRequest> options = java.util.stream.IntStream.rangeClosed(1, 17)
                    .mapToObj(i -> new VoteOptionRequest("선택지" + i, null))
                    .toList();

            final VotePostCreateRequest request = new VotePostCreateRequest(
                    "제목", "내용", null, options, null);

            mockMvc.perform(post(POSTS_URL + "/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 마감된 투표 선택지 오류 → 400")
        void voteOptionNotBelongs() throws Exception {
            given(postService.createVotePost(any(VotePostCreateRequest.class), any(), any()))
                    .willThrow(new CustomException(ErrorCode.VOTE_OPTION_NOT_BELONGS));

            final VotePostCreateRequest request = new VotePostCreateRequest(
                    "제목", "내용", null, twoOptions(), null);

            mockMvc.perform(post(POSTS_URL + "/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── POST /api/posts/debate/{id}/stance ──────────────────

    @Nested
    @DisplayName("POST /api/posts/debate/{id}/stance — 토론 진영 투표")
    class VoteDebateStance {

        @Test
        @DisplayName("성공 — stance=1(찬성) 투표 → 200")
        void successPro() throws Exception {
            willDoNothing().given(postService)
                    .voteDebateStance(eq(POST_ID), any(DebateStanceRequest.class), any());

            mockMvc.perform(post(POSTS_URL + "/debate/" + POST_ID + "/stance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DebateStanceRequest((short) 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — stance=-1(반대) 투표 → 200")
        void successCon() throws Exception {
            willDoNothing().given(postService)
                    .voteDebateStance(eq(POST_ID), any(DebateStanceRequest.class), any());

            mockMvc.perform(post(POSTS_URL + "/debate/" + POST_ID + "/stance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DebateStanceRequest((short) -1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — stance 필드 누락(@NotNull 위반) → 400")
        void missingStance() throws Exception {
            mockMvc.perform(post(POSTS_URL + "/debate/" + POST_ID + "/stance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 마감된 토론에 투표 → 400")
        void debateStanceClosed() throws Exception {
            willThrow(new CustomException(ErrorCode.DEBATE_STANCE_CLOSED))
                    .given(postService).voteDebateStance(eq(POST_ID), any(DebateStanceRequest.class), any());

            mockMvc.perform(post(POSTS_URL + "/debate/" + POST_ID + "/stance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DebateStanceRequest((short) 1))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.DEBATE_STANCE_CLOSED.getMessage()));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 토론 게시글 → 404")
        void debatePostNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND))
                    .given(postService).voteDebateStance(eq(POST_ID), any(DebateStanceRequest.class), any());

            mockMvc.perform(post(POSTS_URL + "/debate/" + POST_ID + "/stance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DebateStanceRequest((short) 1))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── DELETE /api/posts/debate/{id}/stance ──────────────────

    @Nested
    @DisplayName("DELETE /api/posts/debate/{id}/stance — 토론 진영 투표 취소")
    class RemoveDebateStance {

        @Test
        @DisplayName("성공 — 토론 진영 선택 취소 → 200")
        void success() throws Exception {
            willDoNothing().given(postService).removeDebateStance(eq(POST_ID), any());

            mockMvc.perform(delete(POSTS_URL + "/debate/" + POST_ID + "/stance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 토론 게시글 → 404")
        void debatePostNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.DEBATE_POST_NOT_FOUND))
                    .given(postService).removeDebateStance(eq(POST_ID), any());

            mockMvc.perform(delete(POSTS_URL + "/debate/" + POST_ID + "/stance"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── PATCH /api/posts/{id} ──────────────────

    @Nested
    @DisplayName("PATCH /api/posts/{id} — 게시글 수정")
    class UpdatePost {

        @Test
        @DisplayName("성공 — 회원이 게시글 수정 → 200")
        void successAsUser() throws Exception {
            given(postService.updatePost(eq(POST_ID), any(PostUpdateRequest.class), any()))
                    .willReturn(sampleDetail());

            final PostUpdateRequest request = new PostUpdateRequest("수정된 제목", "수정된 내용", null);

            mockMvc.perform(patch(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("제목"));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final PostUpdateRequest request = new PostUpdateRequest("", "내용", null);

            mockMvc.perform(patch(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 권한 없는 회원이 수정 시도 → 403")
        void forbidden() throws Exception {
            given(postService.updatePost(eq(POST_ID), any(PostUpdateRequest.class), any()))
                    .willThrow(new CustomException(ErrorCode.POST_FORBIDDEN));

            final PostUpdateRequest request = new PostUpdateRequest("제목", "내용", null);

            mockMvc.perform(patch(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.POST_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 → 404")
        void postNotFound() throws Exception {
            given(postService.updatePost(eq(POST_ID), any(PostUpdateRequest.class), any()))
                    .willThrow(new CustomException(ErrorCode.POST_NOT_FOUND));

            final PostUpdateRequest request = new PostUpdateRequest("제목", "내용", null);

            mockMvc.perform(patch(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 비회원 비밀번호 불일치 → 403")
        void guestPasswordMismatch() throws Exception {
            given(postService.updatePost(eq(POST_ID), any(PostUpdateRequest.class), any()))
                    .willThrow(new CustomException(ErrorCode.GUEST_PASSWORD_MISMATCH));

            final PostUpdateRequest request = new PostUpdateRequest("제목", "내용", "wrongpass");

            mockMvc.perform(patch(POSTS_URL + "/" + POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── DELETE /api/posts/{id} ──────────────────

    @Nested
    @DisplayName("DELETE /api/posts/{id} — 게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("성공 — 회원이 본인 게시글 삭제 → 200 및 삭제 메시지")
        void successAsUser() throws Exception {
            willDoNothing().given(postService).deletePost(eq(POST_ID), any(), any());

            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("게시글이 삭제되었습니다."));
        }

        @Test
        @DisplayName("성공 — 비회원이 X-Guest-Password 헤더로 삭제 → 200")
        void successAsGuest() throws Exception {
            willDoNothing().given(postService).deletePost(eq(POST_ID), any(), any());

            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID)
                            .header(ApiConstants.HEADER_GUEST_PASSWORD, "pass1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — 권한 없는 회원이 삭제 시도 → 403")
        void forbidden() throws Exception {
            willThrow(new CustomException(ErrorCode.POST_FORBIDDEN))
                    .given(postService).deletePost(eq(POST_ID), any(), any());

            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 게시글 삭제 → 404")
        void postNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.POST_NOT_FOUND))
                    .given(postService).deletePost(eq(POST_ID), any(), any());

            mockMvc.perform(delete(POSTS_URL + "/" + POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
