package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.AdminFunOptionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunOptionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse;
import com.board.kimicetea.domain.mbti.service.AdminFunQuestionService;
import com.board.kimicetea.domain.mbti.service.AdminMbtiQuestionService;
import com.board.kimicetea.domain.mbti.service.AdminMbtiTestStatsScheduler;
import com.board.kimicetea.domain.mbti.service.AdminMbtiTestStatsService;
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
 * AdminMbtiController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: ROLE_ADMIN 인가는 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(AdminMbtiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AdminMbtiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AdminMbtiTestStatsService adminMbtiTestStatsService;
    @MockBean private AdminMbtiTestStatsScheduler adminMbtiTestStatsScheduler;
    @MockBean private AdminMbtiQuestionService adminMbtiQuestionService;
    @MockBean private AdminFunQuestionService adminFunQuestionService;


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

    private static final String STATS_URL              = "/api/admin/mbti/stats";
    private static final String STATS_REFRESH_URL      = "/api/admin/mbti/stats/refresh";
    private static final String QUESTIONS_URL          = "/api/admin/mbti/questions";
    private static final String FUN_QUESTIONS_URL      = "/api/admin/mbti/fun-questions";
    private static final long   QUESTION_ID            = 1L;
    private static final long   FUN_QUESTION_ID        = 10L;

    @BeforeEach
    void setAdminAuth() {
        final CustomUserDetails admin = new CustomUserDetails(
                UUID.randomUUID(), "admin@example.com", null, "ADMIN", "device-admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    private AdminMbtiTestStatsResponse sampleStats() {
        return new AdminMbtiTestStatsResponse(
                500L, 50L, 40L,
                2.5, -1.3, 0.8, -0.4,
                List.of(new AdminMbtiTestStatsResponse.CountEntry("INTJ", 80L)),
                List.of(new AdminMbtiTestStatsResponse.CountEntry("1회", 300L)),
                List.of(new AdminMbtiTestStatsResponse.DailyCountEntry("07-01", 20L)));
    }

    private AdminMbtiQuestionResponse sampleQuestion() {
        return new AdminMbtiQuestionResponse(
                QUESTION_ID, "EI", "당신은 외향적인가요?", false, true, Instant.now());
    }

    private AdminFunQuestionResponse sampleFunQuestion() {
        return new AdminFunQuestionResponse(
                FUN_QUESTION_ID, "재미 질문입니다.",
                List.of(), true, Instant.now());
    }

    // ────────────────── GET /api/admin/mbti/stats ──────────────────

    @Nested
    @DisplayName("GET /api/admin/mbti/stats — MBTI 검사 통계 조회")
    class GetMbtiTestStats {

        @Test
        @DisplayName("성공 — Redis 캐시에서 즉시 반환 → 200")
        void success() throws Exception {
            given(adminMbtiTestStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalTests").value(500))
                    .andExpect(jsonPath("$.data.thisMonthTests").value(50))
                    .andExpect(jsonPath("$.data.lastMonthTests").value(40));
        }

        @Test
        @DisplayName("성공 — 지표 평균 및 byResult·byRetakeCount·testTrend 포함 확인 → 200")
        void successCheckBreakdowns() throws Exception {
            given(adminMbtiTestStatsService.getCachedStats()).willReturn(sampleStats());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.avgEiScore").value(2.5))
                    .andExpect(jsonPath("$.data.byResult").isArray())
                    .andExpect(jsonPath("$.data.byResult[0].name").value("INTJ"))
                    .andExpect(jsonPath("$.data.byRetakeCount").isArray())
                    .andExpect(jsonPath("$.data.testTrend").isArray())
                    .andExpect(jsonPath("$.data.testTrend[0].date").value("07-01"));
        }
    }

    // ────────────────── POST /api/admin/mbti/stats/refresh ──────────────────

    @Nested
    @DisplayName("POST /api/admin/mbti/stats/refresh — MBTI 통계 캐시 수동 갱신")
    class RefreshStats {

        @Test
        @DisplayName("성공 — triggerAsync() 호출 후 202 반환")
        void success() throws Exception {
            willDoNothing().given(adminMbtiTestStatsScheduler).triggerAsync();

            mockMvc.perform(post(STATS_REFRESH_URL))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ────────────────── GET /api/admin/mbti/questions ──────────────────

    @Nested
    @DisplayName("GET /api/admin/mbti/questions — MBTI 전체 문항 조회")
    class GetAllQuestions {

        @Test
        @DisplayName("성공 — is_visible 무관 전체 문항 반환 → 200")
        void success() throws Exception {
            given(adminMbtiQuestionService.getAllQuestions()).willReturn(List.of(sampleQuestion()));

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("성공 — id·category·title·isReverse·isVisible 필드 확인 → 200")
        void successCheckFields() throws Exception {
            given(adminMbtiQuestionService.getAllQuestions()).willReturn(List.of(sampleQuestion()));

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(QUESTION_ID))
                    .andExpect(jsonPath("$.data[0].category").value("EI"))
                    .andExpect(jsonPath("$.data[0].title").value("당신은 외향적인가요?"))
                    .andExpect(jsonPath("$.data[0].isReverse").value(false))
                    .andExpect(jsonPath("$.data[0].isVisible").value(true));
        }

        @Test
        @DisplayName("성공 — 문항이 없을 때 빈 배열 반환 → 200")
        void successEmptyList() throws Exception {
            given(adminMbtiQuestionService.getAllQuestions()).willReturn(List.of());

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ────────────────── POST /api/admin/mbti/questions ──────────────────

    @Nested
    @DisplayName("POST /api/admin/mbti/questions — MBTI 문항 생성")
    class CreateQuestion {

        @Test
        @DisplayName("성공 — 유효한 생성 요청 → 201")
        void success() throws Exception {
            given(adminMbtiQuestionService.createQuestion(any(AdminMbtiQuestionCreateRequest.class)))
                    .willReturn(sampleQuestion());

            final AdminMbtiQuestionCreateRequest request =
                    new AdminMbtiQuestionCreateRequest("새로운 질문입니다.", "EI", false);

            mockMvc.perform(post(QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(QUESTION_ID))
                    .andExpect(jsonPath("$.data.category").value("EI"));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final AdminMbtiQuestionCreateRequest request =
                    new AdminMbtiQuestionCreateRequest("", "EI", false);

            mockMvc.perform(post(QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — category 허용값 외(@Pattern 위반: 'XY') → 400")
        void invalidCategory() throws Exception {
            final AdminMbtiQuestionCreateRequest request =
                    new AdminMbtiQuestionCreateRequest("질문", "XY", false);

            mockMvc.perform(post(QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — isReverse null(@NotNull 위반) → 400")
        void nullIsReverse() throws Exception {
            mockMvc.perform(post(QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"질문\", \"category\": \"EI\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── PATCH /api/admin/mbti/questions/{id} ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/mbti/questions/{id} — MBTI 문항 수정")
    class UpdateQuestion {

        private String questionUrl(final long id) {
            return QUESTIONS_URL + "/" + id;
        }

        @Test
        @DisplayName("성공 — 유효한 수정 요청 → 200")
        void success() throws Exception {
            willDoNothing().given(adminMbtiQuestionService)
                    .updateQuestion(eq(QUESTION_ID), any(AdminMbtiQuestionUpdateRequest.class));

            final AdminMbtiQuestionUpdateRequest request =
                    new AdminMbtiQuestionUpdateRequest("수정된 질문입니다.", "SN", true);

            mockMvc.perform(patch(questionUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final AdminMbtiQuestionUpdateRequest request =
                    new AdminMbtiQuestionUpdateRequest("", "EI", false);

            mockMvc.perform(patch(questionUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — category 허용값 외(@Pattern 위반: 'XY') → 400")
        void invalidCategory() throws Exception {
            final AdminMbtiQuestionUpdateRequest request =
                    new AdminMbtiQuestionUpdateRequest("질문", "XY", false);

            mockMvc.perform(patch(questionUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — isReverse null(@NotNull 위반) → 400")
        void nullIsReverse() throws Exception {
            mockMvc.perform(patch(questionUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"질문\", \"category\": \"EI\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 문항 ID → 404")
        void questionNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.QUESTION_NOT_FOUND))
                    .given(adminMbtiQuestionService)
                    .updateQuestion(eq(QUESTION_ID), any(AdminMbtiQuestionUpdateRequest.class));

            final AdminMbtiQuestionUpdateRequest request =
                    new AdminMbtiQuestionUpdateRequest("질문", "EI", false);

            mockMvc.perform(patch(questionUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.QUESTION_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── PATCH /api/admin/mbti/questions/{id}/visibility ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/mbti/questions/{id}/visibility — MBTI 문항 노출 여부 변경")
    class ChangeQuestionVisibility {

        private String visibilityUrl(final long id) {
            return QUESTIONS_URL + "/" + id + "/visibility";
        }

        @Test
        @DisplayName("성공 — visible=false(숨김) → 200")
        void successHide() throws Exception {
            willDoNothing().given(adminMbtiQuestionService)
                    .changeVisibility(eq(QUESTION_ID), any(AdminMbtiQuestionVisibilityRequest.class));

            mockMvc.perform(patch(visibilityUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminMbtiQuestionVisibilityRequest(false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — visible=true(복구) → 200")
        void successRestore() throws Exception {
            willDoNothing().given(adminMbtiQuestionService)
                    .changeVisibility(eq(QUESTION_ID), any(AdminMbtiQuestionVisibilityRequest.class));

            mockMvc.perform(patch(visibilityUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminMbtiQuestionVisibilityRequest(true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — visible 필드 누락(@NotNull 위반) → 400")
        void missingVisible() throws Exception {
            mockMvc.perform(patch(visibilityUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 문항 ID → 404")
        void questionNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.QUESTION_NOT_FOUND))
                    .given(adminMbtiQuestionService)
                    .changeVisibility(eq(QUESTION_ID), any(AdminMbtiQuestionVisibilityRequest.class));

            mockMvc.perform(patch(visibilityUrl(QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminMbtiQuestionVisibilityRequest(false))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.QUESTION_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── GET /api/admin/mbti/fun-questions ──────────────────

    @Nested
    @DisplayName("GET /api/admin/mbti/fun-questions — FUN 전체 문항 조회")
    class GetAllFunQuestions {

        @Test
        @DisplayName("성공 — is_visible 무관 전체 FUN 문항 반환 → 200")
        void success() throws Exception {
            given(adminFunQuestionService.getAllFunQuestions()).willReturn(List.of(sampleFunQuestion()));

            mockMvc.perform(get(FUN_QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("성공 — id·title·isVisible 필드 확인 → 200")
        void successCheckFields() throws Exception {
            given(adminFunQuestionService.getAllFunQuestions()).willReturn(List.of(sampleFunQuestion()));

            mockMvc.perform(get(FUN_QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(FUN_QUESTION_ID))
                    .andExpect(jsonPath("$.data[0].title").value("재미 질문입니다."))
                    .andExpect(jsonPath("$.data[0].isVisible").value(true));
        }
    }

    // ────────────────── POST /api/admin/mbti/fun-questions ──────────────────

    @Nested
    @DisplayName("POST /api/admin/mbti/fun-questions — FUN 문항 생성")
    class CreateFunQuestion {

        @Test
        @DisplayName("성공 — 유효한 생성 요청(제목 + 선택지 2개 이상) → 201")
        void success() throws Exception {
            given(adminFunQuestionService.createFunQuestion(any(AdminFunQuestionCreateRequest.class)))
                    .willReturn(sampleFunQuestion());

            final AdminFunQuestionCreateRequest request = new AdminFunQuestionCreateRequest(
                    "새로운 재미 질문",
                    List.of(new AdminFunOptionCreateRequest("선택지 A"), new AdminFunOptionCreateRequest("선택지 B")));

            mockMvc.perform(post(FUN_QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(FUN_QUESTION_ID));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final AdminFunQuestionCreateRequest request = new AdminFunQuestionCreateRequest(
                    "",
                    List.of(new AdminFunOptionCreateRequest("선택지 A"), new AdminFunOptionCreateRequest("선택지 B")));

            mockMvc.perform(post(FUN_QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 선택지 1개뿐(@Size(min=2) 위반) → 400")
        void tooFewOptions() throws Exception {
            final AdminFunQuestionCreateRequest request = new AdminFunQuestionCreateRequest(
                    "제목", List.of(new AdminFunOptionCreateRequest("선택지 A")));

            mockMvc.perform(post(FUN_QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 선택지 텍스트 빈 문자열(@NotBlank 위반) → 400")
        void blankOptionText() throws Exception {
            final AdminFunQuestionCreateRequest request = new AdminFunQuestionCreateRequest(
                    "제목", List.of(new AdminFunOptionCreateRequest(""), new AdminFunOptionCreateRequest("선택지 B")));

            mockMvc.perform(post(FUN_QUESTIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── PATCH /api/admin/mbti/fun-questions/{id} ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/mbti/fun-questions/{id} — FUN 문항 수정")
    class UpdateFunQuestion {

        private String funQuestionUrl(final long id) {
            return FUN_QUESTIONS_URL + "/" + id;
        }

        @Test
        @DisplayName("성공 — 유효한 수정 요청(제목·선택지 텍스트) → 200")
        void success() throws Exception {
            willDoNothing().given(adminFunQuestionService)
                    .updateFunQuestion(eq(FUN_QUESTION_ID), any(AdminFunQuestionUpdateRequest.class));

            final AdminFunQuestionUpdateRequest request = new AdminFunQuestionUpdateRequest(
                    "수정된 재미 질문",
                    List.of(new AdminFunOptionUpdateRequest(10L, "수정된 선택지 A")));

            mockMvc.perform(patch(funQuestionUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — title 빈 문자열(@NotBlank 위반) → 400")
        void blankTitle() throws Exception {
            final AdminFunQuestionUpdateRequest request = new AdminFunQuestionUpdateRequest(
                    "",
                    List.of(new AdminFunOptionUpdateRequest(10L, "선택지 A")));

            mockMvc.perform(patch(funQuestionUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — options 빈 배열(@Size(min=1) 위반) → 400")
        void emptyOptions() throws Exception {
            final AdminFunQuestionUpdateRequest request = new AdminFunQuestionUpdateRequest(
                    "제목", List.of());

            mockMvc.perform(patch(funQuestionUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 문항 또는 선택지 ID → 404")
        void questionOrOptionNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.QUESTION_NOT_FOUND))
                    .given(adminFunQuestionService)
                    .updateFunQuestion(eq(FUN_QUESTION_ID), any(AdminFunQuestionUpdateRequest.class));

            final AdminFunQuestionUpdateRequest request = new AdminFunQuestionUpdateRequest(
                    "제목", List.of(new AdminFunOptionUpdateRequest(9999L, "선택지")));

            mockMvc.perform(patch(funQuestionUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.QUESTION_NOT_FOUND.getMessage()));
        }
    }

    // ────────────────── PATCH /api/admin/mbti/fun-questions/{id}/visibility ──────────────────

    @Nested
    @DisplayName("PATCH /api/admin/mbti/fun-questions/{id}/visibility — FUN 문항 노출 여부 변경")
    class ChangeFunQuestionVisibility {

        private String funVisibilityUrl(final long id) {
            return FUN_QUESTIONS_URL + "/" + id + "/visibility";
        }

        @Test
        @DisplayName("성공 — visible=false(숨김) → 200")
        void successHide() throws Exception {
            willDoNothing().given(adminFunQuestionService)
                    .changeVisibility(eq(FUN_QUESTION_ID), any(AdminFunQuestionVisibilityRequest.class));

            mockMvc.perform(patch(funVisibilityUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminFunQuestionVisibilityRequest(false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("성공 — visible=true(복구) → 200")
        void successRestore() throws Exception {
            willDoNothing().given(adminFunQuestionService)
                    .changeVisibility(eq(FUN_QUESTION_ID), any(AdminFunQuestionVisibilityRequest.class));

            mockMvc.perform(patch(funVisibilityUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminFunQuestionVisibilityRequest(true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — visible 필드 누락(@NotNull 위반) → 400")
        void missingVisible() throws Exception {
            mockMvc.perform(patch(funVisibilityUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 FUN 문항 ID → 404")
        void funQuestionNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.QUESTION_NOT_FOUND))
                    .given(adminFunQuestionService)
                    .changeVisibility(eq(FUN_QUESTION_ID), any(AdminFunQuestionVisibilityRequest.class));

            mockMvc.perform(patch(funVisibilityUrl(FUN_QUESTION_ID))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AdminFunQuestionVisibilityRequest(false))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.QUESTION_NOT_FOUND.getMessage()));
        }
    }
}
