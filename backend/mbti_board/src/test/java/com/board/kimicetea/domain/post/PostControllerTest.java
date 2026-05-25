package com.board.kimicetea.domain.post;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.post.dto.PostCreateRequest;
import com.board.kimicetea.domain.post.dto.PostUpdateRequest;
import com.board.kimicetea.domain.post.repository.PostRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 게시글 도메인 통합 테스트 (Phase 2 — FREE 게시판)
 * 주의: 실행 전 docker compose up 으로 postgres:5432, redis:6379 가 떠있어야 합니다.
 * 주의: EmailService는 @MockBean — tokenRedisService.markEmailVerified 로 이메일 인증 우회
 */
@AutoConfigureMockMvc
class PostControllerTest extends IntegrationTestSupport {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;
        @Autowired
        UserRepository userRepository;
        @Autowired
        PostRepository postRepository;
        @Autowired
        TokenRedisService tokenRedisService;
        @Autowired
        StringRedisTemplate redisTemplate;
        @MockBean
        EmailService emailService;

        private static final String TEST_EMAIL = "post-test@example.com";
        private static final String TEST_EMAIL2 = "post-test2@example.com";
        private static final String TEST_PASSWORD = "password123!";
        private static final String TEST_NICKNAME = "게시글테스터";
        private static final String TEST_NICKNAME2 = "게시글테스터2";
        private static final String TEST_MBTI = "INFP";
        private static final String GUEST_PASSWORD = "guest1234";
        /** Tiptap 최소 JSON — DB jsonb 타입에 유효한 값 */
        private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";

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

        /** 회원가입 → 로그인 → AT 추출 */
        private String 회원가입_후_로그인_AT_추출(final String email, final String nickname) throws Exception {
                tokenRedisService.markEmailVerified(email);
                mockMvc.perform(post(SIGNUP_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new SignupRequest(email, TEST_PASSWORD, nickname, TEST_MBTI))))
                                .andExpect(status().isCreated());

                final String body = mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest(email, TEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("accessToken").asText();
        }

        /** 회원 게시글 작성 → 게시글 ID 추출 */
        private long 회원_게시글_작성_후_ID_추출(final String accessToken) throws Exception {
                final String body = mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + accessToken)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("테스트 제목", CONTENT, "FREE", null, null, null, null))))
                                // .andDo(print())
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("id").asLong();
        }

        /** 비회원 게시글 작성 → 게시글 ID 추출 */
        private long 비회원_게시글_작성_후_ID_추출(final String guestPassword) throws Exception {
                final String body = mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("비회원 제목", CONTENT, "FREE", guestPassword, null, null, null))))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();

                return objectMapper.readTree(body).path("data").path("id").asLong();
        }

        // ─────────────────────────────────────────────────────────────
        // 1단계 — 게시글 목록 조회 (GET /api/posts)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("1-1. 게시글 없을 때 목록 조회 → 200, 빈 content 반환")
        void 목록_조회_빈_결과_성공() throws Exception {
                mockMvc.perform(get(POSTS_URL))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("1-2. 게시글 작성 후 목록 조회 → 200, totalElements=1")
        void 목록_조회_게시글_있음_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                회원_게시글_작성_후_ID_추출(at);

                mockMvc.perform(get(POSTS_URL))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(1))
                                .andExpect(jsonPath("$.data.content[0].title").value("테스트 제목"));
        }

        @Test
        @DisplayName("1-3. targetMbti 필터 — 조건 불일치 시 빈 목록 반환")
        void 목록_조회_targetMbti_필터_불일치() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                // targetMbti 없는 게시글 생성 (null = ALL)
                회원_게시글_작성_후_ID_추출(at);

                mockMvc.perform(get(POSTS_URL).param("targetMbti", "INTJ"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("1-4. page=-1 → 400 Bad Request")
        void 목록_조회_page_음수_실패() throws Exception {
                mockMvc.perform(get(POSTS_URL).param("page", "-1"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1-5. size=0 → 400 Bad Request")
        void 목록_조회_size_0_실패() throws Exception {
                mockMvc.perform(get(POSTS_URL).param("size", "0"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("1-6. size=101 → 400 Bad Request")
        void 목록_조회_size_101_실패() throws Exception {
                mockMvc.perform(get(POSTS_URL).param("size", "101"))
                                .andExpect(status().isBadRequest());
        }

        // ─────────────────────────────────────────────────────────────
        // 2단계 — 게시글 단건 조회 (GET /api/posts/{id})
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("2-1. 게시글 단건 조회 성공 → 200, viewCount 증가 확인")
        void 단건_조회_성공_viewCount_증가() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final long id = 회원_게시글_작성_후_ID_추출(at);

                // 첫 조회: viewCount=0 반환, DB는 1로 증가
                mockMvc.perform(get(POSTS_URL + "/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(id))
                                .andExpect(jsonPath("$.data.viewCount").value(0));

                // 두 번째 조회: viewCount=1 반환 (이전 요청에서 증가된 값)
                mockMvc.perform(get(POSTS_URL + "/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.viewCount").value(1));
        }

        @Test
        @DisplayName("2-2. 존재하지 않는 게시글 조회 → 404, '게시글을 찾을 수 없습니다.'")
        void 단건_조회_없는_게시글_실패() throws Exception {
                mockMvc.perform(get(POSTS_URL + "/99999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("2-3. 회원이 본인 글 조회 → isOwner=true")
        void 단건_조회_회원_본인_글_isOwner_true() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final long id = 회원_게시글_작성_후_ID_추출(at);

                mockMvc.perform(get(POSTS_URL + "/" + id)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.isOwner").value(true))
                                .andExpect(jsonPath("$.data.isGuest").value(false));
        }

        @Test
        @DisplayName("2-4. 비회원 글 조회 → isGuest=true, isOwner=false")
        void 단건_조회_비회원_글_isGuest_true() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(get(POSTS_URL + "/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.isGuest").value(true))
                                .andExpect(jsonPath("$.data.isOwner").value(false));
        }

        // ─────────────────────────────────────────────────────────────
        // 3단계 — 게시글 작성 (POST /api/posts)
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("3-1. 회원 게시글 작성 성공 → 201, isOwner=true, isGuest=false")
        void 게시글_작성_회원_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("회원 작성글", CONTENT, "FREE", null, null, null, null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.title").value("회원 작성글"))
                                .andExpect(jsonPath("$.data.isOwner").value(true))
                                .andExpect(jsonPath("$.data.isGuest").value(false));
        }

        @Test
        @DisplayName("3-2. 비회원 게시글 작성 성공 → 201, isGuest=true, authorMbtiId=NONE")
        void 게시글_작성_비회원_성공() throws Exception {
                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("비회원 작성글", CONTENT, "FREE", GUEST_PASSWORD,
                                                                null, null, null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.isGuest").value(true))
                                .andExpect(jsonPath("$.data.authorMbtiId").value("NONE"));
        }

        @Test
        @DisplayName("3-3. 비회원 게시글 작성 시 guestPassword 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
        void 게시글_작성_비회원_비밀번호_없음_실패() throws Exception {
                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("비회원 작성글", CONTENT, "FREE", null, null, null, null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
        }

        @Test
        @DisplayName("3-4. 존재하지 않는 type으로 작성 → 400, '존재하지 않는 게시판 유형입니다.'")
        void 게시글_작성_잘못된_type_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("제목", CONTENT, "INVALID_TYPE", null, null, null, null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("존재하지 않는 게시판 유형입니다."));
        }

        @Test
        @DisplayName("3-5. title 공백으로 작성 → 400 Bad Request")
        void 게시글_작성_title_공백_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("   ", CONTENT, "FREE", null, null, null, null))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("3-6. targetMbti 지정 작성 성공 → 201, targetMbtiId 반환")
        void 게시글_작성_targetMbti_지정_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("INFP에게", CONTENT, "FREE", null, "INFP", null, null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.targetMbtiId").value("INFP"));
        }

        @Test
        @DisplayName("3-7. targetMbti=ALL 지정 시 targetMbtiId=null 반환")
        void 게시글_작성_targetMbti_ALL_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(post(POSTS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostCreateRequest("전체 대상", CONTENT, "FREE", null, "ALL", null, null))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.targetMbtiId").doesNotExist());
        }

        // ─────────────────────────────────────────────────────────────
        // 4단계 — 게시글 수정 (PATCH /api/posts/{id})
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("4-1. 회원 본인 게시글 수정 성공 → 200, 변경된 title 반환")
        void 게시글_수정_회원_본인_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final long id = 회원_게시글_작성_후_ID_추출(at);

                mockMvc.perform(patch(POSTS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정된 제목", CONTENT, null))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
        }

        @Test
        @DisplayName("4-2. 다른 회원이 게시글 수정 → 403, '게시글 수정/삭제 권한이 없습니다.'")
        void 게시글_수정_다른_회원_실패() throws Exception {
                final String at1 = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final String at2 = 회원가입_후_로그인_AT_추출(TEST_EMAIL2, TEST_NICKNAME2);
                final long id = 회원_게시글_작성_후_ID_추출(at1);

                mockMvc.perform(patch(POSTS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at2)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정 시도", CONTENT, null))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("게시글 수정/삭제 권한이 없습니다."));
        }

        @Test
        @DisplayName("4-3. 비회원 올바른 비밀번호로 수정 성공 → 200")
        void 게시글_수정_비회원_올바른_비밀번호_성공() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(patch(POSTS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정된 비회원 제목", CONTENT, GUEST_PASSWORD))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.title").value("수정된 비회원 제목"));
        }

        @Test
        @DisplayName("4-4. 비회원 틀린 비밀번호로 수정 → 403, '비밀번호가 일치하지 않습니다.'")
        void 게시글_수정_비회원_틀린_비밀번호_실패() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(patch(POSTS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정 시도", CONTENT, "wrongpassword"))))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
        }

        @Test
        @DisplayName("4-5. 비회원 글 수정 시 비밀번호 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
        void 게시글_수정_비회원_비밀번호_없음_실패() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(patch(POSTS_URL + "/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정 시도", CONTENT, null))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
        }

        @Test
        @DisplayName("4-6. 존재하지 않는 게시글 수정 → 404, '게시글을 찾을 수 없습니다.'")
        void 게시글_수정_없는_게시글_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(patch(POSTS_URL + "/99999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + at)
                                .content(objectMapper.writeValueAsString(
                                                new PostUpdateRequest("수정 시도", CONTENT, null))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }

        // ─────────────────────────────────────────────────────────────
        // 5단계 — 게시글 삭제 (DELETE /api/posts/{id})
        // ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("5-1. 회원 본인 게시글 삭제 성공 → 200, 이후 단건 조회 404")
        void 게시글_삭제_회원_본인_성공() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final long id = 회원_게시글_작성_후_ID_추출(at);

                mockMvc.perform(delete(POSTS_URL + "/" + id)
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // 소프트 삭제 — isVisible=false → 단건 조회 시 404
                mockMvc.perform(get(POSTS_URL + "/" + id))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("5-2. 다른 회원이 게시글 삭제 → 403, '게시글 수정/삭제 권한이 없습니다.'")
        void 게시글_삭제_다른_회원_실패() throws Exception {
                final String at1 = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);
                final String at2 = 회원가입_후_로그인_AT_추출(TEST_EMAIL2, TEST_NICKNAME2);
                final long id = 회원_게시글_작성_후_ID_추출(at1);

                mockMvc.perform(delete(POSTS_URL + "/" + id)
                                .header("Authorization", "Bearer " + at2))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("게시글 수정/삭제 권한이 없습니다."));
        }

        @Test
        @DisplayName("5-3. 비회원 올바른 비밀번호로 삭제 성공 → 200")
        void 게시글_삭제_비회원_올바른_비밀번호_성공() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(delete(POSTS_URL + "/" + id)
                                .header("X-Guest-Password", GUEST_PASSWORD))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                mockMvc.perform(get(POSTS_URL + "/" + id))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("5-4. 비회원 틀린 비밀번호로 삭제 → 403, '비밀번호가 일치하지 않습니다.'")
        void 게시글_삭제_비회원_틀린_비밀번호_실패() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(delete(POSTS_URL + "/" + id)
                                .header("X-Guest-Password", "wrongpassword"))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
        }

        @Test
        @DisplayName("5-5. 비회원 글 삭제 시 X-Guest-Password 헤더 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
        void 게시글_삭제_비회원_헤더_없음_실패() throws Exception {
                final long id = 비회원_게시글_작성_후_ID_추출(GUEST_PASSWORD);

                mockMvc.perform(delete(POSTS_URL + "/" + id))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("비회원은 비밀번호를 입력해야 합니다."));
        }

        @Test
        @DisplayName("5-6. 존재하지 않는 게시글 삭제 → 404, '게시글을 찾을 수 없습니다.'")
        void 게시글_삭제_없는_게시글_실패() throws Exception {
                final String at = 회원가입_후_로그인_AT_추출(TEST_EMAIL, TEST_NICKNAME);

                mockMvc.perform(delete(POSTS_URL + "/99999")
                                .header("Authorization", "Bearer " + at))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        }
}
