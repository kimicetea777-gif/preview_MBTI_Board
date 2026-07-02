package com.board.kimicetea.global.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 슬라이스 테스트 — standaloneSetup으로 Spring 전체 컨텍스트 없이 검증
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class DummyController {

        @GetMapping("/test/custom")
        void custom() {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        @GetMapping("/test/integrity")
        void integrity() {
            throw new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"users_email_key\"\n" +
                    "Detail: Key (email)=(test@test.com) already exists.");
        }

        @GetMapping("/test/server-error")
        void serverError() {
            throw new RuntimeException("unexpected");
        }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody ValidBody body) {}

        record ValidBody(@NotBlank String name) {}
    }

    @Nested
    @DisplayName("CustomException — 지정된 HTTP 상태코드 반환")
    class HandleCustomException {

        @Test
        @DisplayName("USER_NOT_FOUND → 404와 success=false 반환")
        void userNotFound_returns404() throws Exception {
            mockMvc.perform(get("/test/custom"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
        }
    }

    @Nested
    @DisplayName("DataIntegrityViolationException — 409 반환 및 PII 비노출")
    class HandleDataIntegrityViolation {

        @Test
        @DisplayName("UNIQUE 제약 위반 → 409와 고정 메시지 반환")
        void uniqueViolation_returns409() throws Exception {
            mockMvc.perform(get("/test/integrity"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 사용 중 입니다."));
        }

        @Test
        @DisplayName("응답 메시지에 이메일(PII)이 포함되지 않는다")
        void response_doesNotContainPii() throws Exception {
            mockMvc.perform(get("/test/integrity"))
                    .andExpect(jsonPath("$.message").value("이미 사용 중 입니다."));
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException — 400 반환")
    class HandleValidationException {

        @Test
        @DisplayName("@NotBlank 위반 → 400과 success=false 반환")
        void notBlank_returns400() throws Exception {
            mockMvc.perform(post("/test/valid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Exception — 500 반환")
    class HandleException {

        @Test
        @DisplayName("예상치 못한 예외 → 500과 success=false 반환")
        void unexpectedException_returns500() throws Exception {
            mockMvc.perform(get("/test/server-error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
