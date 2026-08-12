package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.FunResponseSubmitRequest;
import com.board.kimicetea.domain.mbti.dto.FunStatsResponse;
import com.board.kimicetea.domain.mbti.service.FunService;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FunController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: 두 엔드포인트 모두 인증 불필요 — 비회원 포함
 */
@WebMvcTest(FunController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private FunService funService;


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

    private static final String RESPONSES_URL = "/api/mbti/fun/responses";
    private static final String STATS_URL      = "/api/mbti/fun/stats";

    private FunStatsResponse sampleStats() {
        final List<FunStatsResponse.MbtiCount> mbtiCounts = List.of(
                new FunStatsResponse.MbtiCount("INTJ", 10L),
                new FunStatsResponse.MbtiCount("ENFP", 5L));
        final FunStatsResponse.OptionStats optionStats = new FunStatsResponse.OptionStats(
                10L, "선택지 A", 1, 15L, mbtiCounts);
        return new FunStatsResponse(1L, "재미 질문입니다.", List.of(optionStats));
    }

    // ────────────────── POST /api/mbti/fun/responses ──────────────────

    @Nested
    @DisplayName("POST /api/mbti/fun/responses — 재미 질문 응답 저장")
    class SubmitResponses {

        @Test
        @DisplayName("성공 — 유효한 mbtiId·optionIds 제출 → 200")
        void success() throws Exception {
            willDoNothing().given(funService).submitResponses(any(FunResponseSubmitRequest.class));

            final FunResponseSubmitRequest request = new FunResponseSubmitRequest(
                    "INTJ", List.of(10L, 20L));

            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패 — mbtiId 형식 오류(@Pattern 위반: 소문자) → 400")
        void invalidMbtiIdLowerCase() throws Exception {
            final FunResponseSubmitRequest request = new FunResponseSubmitRequest(
                    "intj", List.of(10L));

            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — mbtiId null(@NotNull 위반) → 400")
        void nullMbtiId() throws Exception {
            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"optionIds\": [10]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — optionIds 빈 배열(@NotEmpty 위반) → 400")
        void emptyOptionIds() throws Exception {
            final FunResponseSubmitRequest request = new FunResponseSubmitRequest(
                    "INTJ", List.of());

            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — optionIds 10개 초과(@Size(max=10) 위반) → 400")
        void tooManyOptionIds() throws Exception {
            final List<Long> elevenIds = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
            final FunResponseSubmitRequest request = new FunResponseSubmitRequest("INTJ", elevenIds);

            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 존재하지 않는 optionId → 404")
        void optionNotFound() throws Exception {
            willThrow(new CustomException(ErrorCode.QUESTION_NOT_FOUND))
                    .given(funService).submitResponses(any(FunResponseSubmitRequest.class));

            final FunResponseSubmitRequest request = new FunResponseSubmitRequest(
                    "INTJ", List.of(9999L));

            mockMvc.perform(post(RESPONSES_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ────────────────── GET /api/mbti/fun/stats ──────────────────

    @Nested
    @DisplayName("GET /api/mbti/fun/stats — 재미 질문 전체 통계 조회")
    class GetAllStats {

        @Test
        @DisplayName("성공 — 전체 FUN 통계 반환 → 200")
        void success() throws Exception {
            given(funService.getAllStats()).willReturn(List.of(sampleStats()));

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("성공 — questionId·title·options 필드 확인 → 200")
        void successCheckFields() throws Exception {
            given(funService.getAllStats()).willReturn(List.of(sampleStats()));

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].questionId").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("재미 질문입니다."))
                    .andExpect(jsonPath("$.data[0].options").isArray())
                    .andExpect(jsonPath("$.data[0].options[0].totalCount").value(15));
        }

        @Test
        @DisplayName("성공 — mbtiCounts 배열 포함 및 INTJ 카운트 확인 → 200")
        void successCheckMbtiCounts() throws Exception {
            given(funService.getAllStats()).willReturn(List.of(sampleStats()));

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].options[0].mbtiCounts").isArray())
                    .andExpect(jsonPath("$.data[0].options[0].mbtiCounts[0].mbtiId").value("INTJ"))
                    .andExpect(jsonPath("$.data[0].options[0].mbtiCounts[0].count").value(10));
        }

        @Test
        @DisplayName("성공 — 데이터가 없을 때 빈 배열 반환 → 200")
        void successEmptyList() throws Exception {
            given(funService.getAllStats()).willReturn(List.of());

            mockMvc.perform(get(STATS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
