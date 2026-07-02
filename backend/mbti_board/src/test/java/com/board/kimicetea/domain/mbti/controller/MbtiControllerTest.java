package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiTestResultResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestSubmitRequest;
import com.board.kimicetea.domain.mbti.dto.MbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.FunOptionResponse;
import com.board.kimicetea.domain.mbti.dto.FunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.QuestionResponse;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MbtiController 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — Security 필터 비활성화(@AutoConfigureMockMvc addFilters=false)
 * 주의: JWT 인증은 SecurityConfig에서 담보 — 슬라이스에서는 서비스 레이어 동작만 검증
 */
@WebMvcTest(MbtiController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MbtiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private MbtiService mbtiService;

    @MockBean private JwtProvider jwtProvider;
    @MockBean private TokenRedisService tokenRedisService;
    @MockBean private CustomOAuth2UserService customOAuth2UserService;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockBean private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String QUESTIONS_URL = "/api/mbti/test/questions";
    private static final String SUBMIT_URL    = "/api/mbti/test/submit";

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUserAuth() {
        userDetails = new CustomUserDetails(
                UUID.randomUUID(), "user@example.com", null, "USER", "device-user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private List<QuestionResponse> sampleQuestions() {
        final MbtiQuestionResponse mbti = new MbtiQuestionResponse("MBTI", 1L, "EI", "당신은 외향적인가요?", false);
        final FunQuestionResponse fun = new FunQuestionResponse(
                "FUN", 2L, "재미 질문입니다.",
                List.of(new FunOptionResponse(10L, "선택지 A", 1)));
        return List.of(mbti, fun);
    }

    // ────────────────── GET /api/mbti/test/questions ──────────────────

    @Nested
    @DisplayName("GET /api/mbti/test/questions — MBTI 검사 문항 조회")
    class GetTestQuestions {

        @Test
        @DisplayName("성공 — MBTI + FUN 혼합 문항 반환 → 200")
        void success() throws Exception {
            given(mbtiService.getTestQuestions()).willReturn(sampleQuestions());

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("성공 — 첫 번째 문항 type=MBTI, category=EI 확인 → 200")
        void successCheckMbtiQuestion() throws Exception {
            given(mbtiService.getTestQuestions()).willReturn(sampleQuestions());

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].type").value("MBTI"))
                    .andExpect(jsonPath("$.data[0].category").value("EI"))
                    .andExpect(jsonPath("$.data[0].id").value(1));
        }

        @Test
        @DisplayName("성공 — 두 번째 문항 type=FUN, options 배열 포함 확인 → 200")
        void successCheckFunQuestion() throws Exception {
            given(mbtiService.getTestQuestions()).willReturn(sampleQuestions());

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[1].type").value("FUN"))
                    .andExpect(jsonPath("$.data[1].options").isArray())
                    .andExpect(jsonPath("$.data[1].options[0].optionText").value("선택지 A"));
        }

        @Test
        @DisplayName("실패 — 출제 가능한 문항 부족 시 → 503")
        void insufficientQuestions() throws Exception {
            given(mbtiService.getTestQuestions())
                    .willThrow(new CustomException(ErrorCode.INSUFFICIENT_VISIBLE_QUESTIONS));

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_VISIBLE_QUESTIONS.getMessage()));
        }

        @Test
        @DisplayName("성공 — 빈 문항 목록 반환 → 200")
        void successEmptyList() throws Exception {
            given(mbtiService.getTestQuestions()).willReturn(List.of());

            mockMvc.perform(get(QUESTIONS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ────────────────── POST /api/mbti/test/submit ──────────────────

    @Nested
    @DisplayName("POST /api/mbti/test/submit — MBTI 검사 결과 제출")
    class SubmitTest {

        private MbtiTestResultResponse sampleResult() {
            return new MbtiTestResultResponse("INTJ", (short) 8, (short) -4, (short) 6, (short) -2);
        }

        @Test
        @DisplayName("성공 — 유효한 점수 제출 → 200 및 MBTI 결과 반환")
        void success() throws Exception {
            given(mbtiService.submitTest(eq(userDetails.userId()), any(MbtiTestSubmitRequest.class)))
                    .willReturn(sampleResult());

            final MbtiTestSubmitRequest request = new MbtiTestSubmitRequest(
                    (short) 8, (short) -4, (short) 6, (short) -2);

            mockMvc.perform(post(SUBMIT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.mbtiId").value("INTJ"))
                    .andExpect(jsonPath("$.data.eiScore").value(8))
                    .andExpect(jsonPath("$.data.snScore").value(-4))
                    .andExpect(jsonPath("$.data.tfScore").value(6))
                    .andExpect(jsonPath("$.data.jpScore").value(-2));
        }

        @Test
        @DisplayName("실패 — eiScore 16 초과(@Max 위반) → 400")
        void eiScoreExceedsMax() throws Exception {
            final MbtiTestSubmitRequest request = new MbtiTestSubmitRequest(
                    (short) 17, (short) 0, (short) 0, (short) 0);

            mockMvc.perform(post(SUBMIT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — snScore -16 미만(@Min 위반) → 400")
        void snScoreBelowMin() throws Exception {
            final MbtiTestSubmitRequest request = new MbtiTestSubmitRequest(
                    (short) 0, (short) -17, (short) 0, (short) 0);

            mockMvc.perform(post(SUBMIT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 필수 필드 누락(@NotNull 위반) → 400")
        void missingRequiredField() throws Exception {
            mockMvc.perform(post(SUBMIT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"eiScore\": 5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("성공 — 경계값 점수(+16, -16, 0, 0) 제출 → 200")
        void boundaryScores() throws Exception {
            given(mbtiService.submitTest(eq(userDetails.userId()), any(MbtiTestSubmitRequest.class)))
                    .willReturn(new MbtiTestResultResponse("ENFP", (short) 16, (short) -16, (short) 0, (short) 0));

            final MbtiTestSubmitRequest request = new MbtiTestSubmitRequest(
                    (short) 16, (short) -16, (short) 0, (short) 0);

            mockMvc.perform(post(SUBMIT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.mbtiId").value("ENFP"));
        }
    }
}
