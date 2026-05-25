package com.board.kimicetea.domain.post;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.VoteOptionRequest;
import com.board.kimicetea.domain.post.dto.VoteRequest;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.domain.post.repository.VotePostHistoryRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 투표 게시판 통합 테스트 (Phase 4)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 * 주의: 마감 투표 테스트(2-5, 3-4)는 endsAt을 100ms 후로 설정 후 200ms sleep — CI 환경에서 flaky 주의
 */
@AutoConfigureMockMvc
class VoteControllerTest extends IntegrationTestSupport {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;
        @Autowired
        UserRepository userRepository;
        @Autowired
        PostRepository postRepository;
        @Autowired
        VotePostHistoryRepository votePostHistoryRepository;
        @Autowired
        TokenRedisService tokenRedisService;
        @Autowired
        StringRedisTemplate redisTemplate;
        @MockBean
        EmailService emailService;

        private static final String TEST_EMAIL = "vote-test@example.com";
        private static final String TEST_PASSWORD = "password123!";
        private static final String TEST_NICKNAME = "투표테스터";
        private static final String TEST_MBTI = "INFP";
        private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";

        private static final List<VoteOptionRequest> TWO_OPTIONS = List.of(
                        new VoteOptionRequest("선택지 A", null),
                        new VoteOptionRequest("선택지 B", null));

        @AfterEach
        void tearDown() {
                // FK: vote_posts_history → vote_post_options → vote_posts → posts
                votePostHistoryRepository.deleteAll();
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

        private String 회원가입_후_로그인_AT_추출() throws Exception {
                tokenRedisService.markEmailVerified(TEST_EMAIL);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(TEST_EMAIL, TEST_PASSWORD, TEST_NICKNAME,
                                                                TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("accessToken").asText();
        }

        /**
         * 투표 게시글 작성 후 [postId, optionId1, optionId2] 반환
         * options의 displayOrder 순서대로 [1]=옵션A, [2]=옵션B
         */
        private long[] 투표_게시글_작성_후_ID_배열_추출(final String accessToken,
                        final List<VoteOptionRequest> options, final Instant endsAt) throws Exception {
                final String body = mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + accessToken)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null, options,
                                                                endsAt))))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                final var data = objectMapper.readTree(body).path("data");
                final long postId = data.path("id").asLong();
                final long optionId1 = data.path("voteResult").path("options").get(0).path("id").asLong();
                final long optionId2 = data.path("voteResult").path("options").get(1).path("id").asLong();
                return new long[] { postId, optionId1, optionId2 };
        }

        private long[] 투표_게시글_작성_후_ID_배열_추출(final String accessToken) throws Exception {
                return 투표_게시글_작성_후_ID_배열_추출(accessToken, TWO_OPTIONS, null);
        }

        private void 투표_참여(final long postId, final long optionId, final String accessToken) throws Exception {
                mockMvc.perform(post(POSTS_URL + "/" + postId + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + accessToken)
                                .content(objectMapper.writeValueAsString(new VoteRequest(optionId))))
                                .andExpect(status().isOk());
        }

        // ─────────────────────────────────────────────────────────────
        // 1단계 — 투표 게시글 작성 (POST /api/posts with type=VOTE)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-1. 선택지 2개로 정상 작성 → 201, voteResult.options 포함, isEnded=false")
        void 투표_게시글_작성_선택지2개_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null, TWO_OPTIONS,
                                                                null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voteResult.options").isArray())
                                .andExpect(jsonPath("$.data.voteResult.options.length()").value(2))
                                .andExpect(jsonPath("$.data.voteResult.isEnded").value(false));
        }

        @Test
        @DisplayName("1-2. 선택지 16개로 작성 (최대 — MBTI 모드 대응) → 201")
        void 투표_게시글_작성_선택지16개_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final List<VoteOptionRequest> sixteenOptions = IntStream.rangeClosed(1, 16)
                                .mapToObj(i -> new VoteOptionRequest("선택지 " + i, null))
                                .toList();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null, sixteenOptions,
                                                                null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voteResult.options.length()").value(16));
        }

        @Test
        @DisplayName("1-3. 선택지 17개로 작성 (최대 초과) → 400 Bad Request")
        void 투표_게시글_작성_선택지17개_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final List<VoteOptionRequest> seventeenOptions = IntStream.rangeClosed(1, 17)
                                .mapToObj(i -> new VoteOptionRequest("선택지 " + i, null))
                                .toList();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null,
                                                                seventeenOptions, null))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1-4. voteOptions 없음 → 400, '투표 게시글은 최소 2개 이상의 선택지가 필요합니다.'")
        void 투표_게시글_작성_선택지없음_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null, null,
                                                                null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("투표 게시글은 최소 2개 이상의 선택지가 필요합니다."));
        }

        @Test
        @DisplayName("1-5. 선택지 1개 (최소 미달) → 400, '투표 게시글은 최소 2개 이상의 선택지가 필요합니다.'")
        void 투표_게시글_작성_선택지1개_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null,
                                                                List.of(new VoteOptionRequest("하나뿐", null)), null))))
                                .andExpect(status().isBadRequest());
                // .andExpect(jsonPath("$.message").value("투표 게시글은 최소 2개 이상의 선택지가 필요합니다."));
        }

        @Test
        @DisplayName("1-6. endsAt이 과거 시각 → 400, '마감일은 현재 시각보다 이후여야 합니다.'")
        void 투표_게시글_작성_과거_마감일_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null,
                                                                TWO_OPTIONS, Instant.now().minusSeconds(60)))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("마감일은 현재 시각보다 이후여야 합니다."));
        }

        @Test
        @DisplayName("1-7. MBTI 선택지 + 일반 선택지 혼합 → 400, 'MBTI 선택지와 일반 선택지를 혼합할 수 없습니다.'")
        void 투표_게시글_작성_MBTI_일반_혼합_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null,
                                                                List.of(
                                                                                new VoteOptionRequest("INFP 선택지",
                                                                                                "INFP"),
                                                                                new VoteOptionRequest("일반 선택지", null)),
                                                                null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("MBTI 선택지와 일반 선택지를 혼합할 수 없습니다."));
        }

        @Test
        @DisplayName("1-8. MBTI 선택지 중 동일 MBTI 중복 → 400, '같은 투표에 동일한 MBTI 선택지를 중복 등록할 수 없습니다.'")
        void 투표_게시글_작성_MBTI_중복_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("투표 제목", CONTENT, "VOTE", null, null,
                                                                List.of(
                                                                                new VoteOptionRequest("INFP 첫번째",
                                                                                                "INFP"),
                                                                                new VoteOptionRequest("INFP 두번째",
                                                                                                "INFP")),
                                                                null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("같은 투표에 동일한 MBTI 선택지를 중복 등록할 수 없습니다."));
        }

        @Test
        @DisplayName("1-9. endsAt 없음 (무기한 투표) → 201, endsAt=null, isEnded=false")
        void 투표_게시글_작성_무기한_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("무기한 투표", CONTENT, "VOTE", null, null,
                                                                TWO_OPTIONS, null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.voteResult.endsAt").doesNotExist())
                                .andExpect(jsonPath("$.data.voteResult.isEnded").value(false));
        }

        // ─────────────────────────────────────────────────────────────
        // 2단계 — 투표 참여 (POST /api/posts/{postId}/vote)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("2-1. 회원 정상 투표 → 200, myVotedOptionId 반환, 해당 선택지 voteCount=1")
        void 투표_참여_회원_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);
                final long postId = ids[0], optionId = ids[1];

                mockMvc.perform(post(POSTS_URL + "/" + postId + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(optionId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").value(optionId))
                                .andExpect(jsonPath("$.data.options[0].voteCount").value(1));
        }

        @Test
        @DisplayName("2-2. 비인증 접근 → 401 Unauthorized")
        void 투표_참여_비인증_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);

                mockMvc.perform(post(POSTS_URL + "/" + ids[0] + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new VoteRequest(ids[1]))))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("2-3. 이미 투표 후 다른 선택지로 변경 → 200, 기존 voteCount=0 · 새 voteCount=1")
        void 투표_참여_선택지_변경_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);
                final long postId = ids[0], optionId1 = ids[1], optionId2 = ids[2];

                투표_참여(postId, optionId1, at);

                mockMvc.perform(post(POSTS_URL + "/" + postId + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(optionId2))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").value(optionId2))
                                .andExpect(jsonPath("$.data.options[0].voteCount").value(0))
                                .andExpect(jsonPath("$.data.options[1].voteCount").value(1));
        }

        @Test
        @DisplayName("2-4. 이미 투표 후 같은 선택지 재선택 → 200, voteCount 변화 없음")
        void 투표_참여_동일_선택지_재선택_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);
                final long postId = ids[0], optionId = ids[1];

                투표_참여(postId, optionId, at);

                mockMvc.perform(post(POSTS_URL + "/" + postId + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(optionId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").value(optionId))
                                .andExpect(jsonPath("$.data.options[0].voteCount").value(1));
        }

        @Test
        @DisplayName("2-5. 마감된 투표에 참여 → 400, '이미 마감된 투표입니다.'")
        void 투표_참여_마감된_투표_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at, TWO_OPTIONS, Instant.now().plusMillis(100));

                Thread.sleep(200);

                mockMvc.perform(post(POSTS_URL + "/" + ids[0] + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(ids[1]))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("이미 마감된 투표입니다."));
        }

        @Test
        @DisplayName("2-6. 존재하지 않는 게시글 ID → 404, '게시글을 찾을 수 없습니다.'")
        void 투표_참여_없는_게시글_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();

                mockMvc.perform(post(POSTS_URL + "/99999/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(1L))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("2-7. FREE 게시글에 투표 요청 → 404, '투표 게시글을 찾을 수 없습니다.'")
        void 투표_참여_FREE_게시글_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final String body = mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("자유글", CONTENT, "FREE", null, null, null, null))))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                final long freePostId = objectMapper.readTree(body).path("data").path("id").asLong();

                mockMvc.perform(post(POSTS_URL + "/" + freePostId + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(1L))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("투표 게시글을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("2-8. 존재하지 않는 선택지 ID → 404, '투표 선택지를 찾을 수 없습니다.'")
        void 투표_참여_없는_선택지_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);

                mockMvc.perform(post(POSTS_URL + "/" + ids[0] + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(99999L))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("투표 선택지를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("2-9. 다른 게시글 소속 선택지 ID 사용 → 400, '선택지가 해당 투표 게시글에 속하지 않습니다.'")
        void 투표_참여_다른_게시글_선택지_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids1 = 투표_게시글_작성_후_ID_배열_추출(at);
                final long[] ids2 = 투표_게시글_작성_후_ID_배열_추출(at);

                // ids2의 선택지로 ids1 게시글에 투표 시도
                mockMvc.perform(post(POSTS_URL + "/" + ids1[0] + "/vote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(new VoteRequest(ids2[1]))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("선택지가 해당 투표 게시글에 속하지 않습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 3단계 — 투표 결과 조회 (GET /api/posts/{postId}/vote/result)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("3-1. 비인증 조회 → 200, myVotedOptionId=null")
        void 투표_결과_조회_비인증_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);

                mockMvc.perform(get(POSTS_URL + "/" + ids[0] + "/vote/result"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").doesNotExist());
        }

        @Test
        @DisplayName("3-2. 인증했지만 미투표 상태 조회 → 200, myVotedOptionId=null")
        void 투표_결과_조회_미투표_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);

                mockMvc.perform(get(POSTS_URL + "/" + ids[0] + "/vote/result")
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").doesNotExist());
        }

        @Test
        @DisplayName("3-3. 투표 후 결과 조회 → 200, myVotedOptionId 반환, voteCount 반영")
        void 투표_결과_조회_투표_후_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at);
                final long postId = ids[0], optionId = ids[1];

                투표_참여(postId, optionId, at);

                mockMvc.perform(get(POSTS_URL + "/" + postId + "/vote/result")
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.myVotedOptionId").value(optionId))
                                .andExpect(jsonPath("$.data.options[0].voteCount").value(1));
        }

        @Test
        @DisplayName("3-4. 마감된 투표 결과 조회 → 200, isEnded=true")
        void 투표_결과_조회_마감_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출();
                final long[] ids = 투표_게시글_작성_후_ID_배열_추출(at, TWO_OPTIONS, Instant.now().plusMillis(100));

                Thread.sleep(200);

                mockMvc.perform(get(POSTS_URL + "/" + ids[0] + "/vote/result"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.isEnded").value(true));
        }

        @Test
        @DisplayName("3-5. 존재하지 않는 게시글 ID → 404, '게시글을 찾을 수 없습니다.'")
        void 투표_결과_조회_없는_게시글_실패() throws Exception {
                mockMvc.perform(get(POSTS_URL + "/99999/vote/result"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }
}
