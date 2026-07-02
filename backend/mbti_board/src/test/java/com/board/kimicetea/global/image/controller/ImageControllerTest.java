package com.board.kimicetea.global.image.controller;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.internal.InternalApiConstants;
import com.board.kimicetea.global.internal.InternalOnlyAspect;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.board.kimicetea.global.image.service.ImageService;
import com.board.kimicetea.global.security.handler.JwtAccessDeniedHandler;
import com.board.kimicetea.global.security.handler.JwtAuthenticationEntryPoint;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.oauth2.CustomOAuth2UserService;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.board.kimicetea.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ImageController 컨트롤러 슬라이스 테스트
 * 주의: @WebMvcTest 슬라이스 — AOP(@RateLimit)는 로드되지 않으므로 Rate Limit 동작은 통합 테스트에서 검증
 */
@WebMvcTest(ImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(InternalOnlyAspect.class)
@EnableAspectJAutoProxy
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageService imageService;

    @MockBean
    private ImageCleanupService imageCleanupService;

    // SecurityConfig 의존성 — @WebMvcTest가 Security 설정을 로드하므로 필수
    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private TokenRedisService tokenRedisService;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String UPLOAD_URL        = "/api/images/upload";
    private static final String CANCEL_URL        = "/api/images";
    private static final String VALID_KEY         = "test-internal-api-key";
    private static final String WRONG_KEY         = "wrong-key";
    private static final String UPLOADED_IMAGE_URL = "http://test-r2.local/images/uuid.png";
    private static final String IMAGE_URL_A       = "http://test-r2.local/images/a.png";
    private static final String IMAGE_URL_B       = "http://test-r2.local/images/b.png";
    private static final String IMAGE_URL_C       = "http://test-r2.local/images/c.png";
    private static final String IMAGE_URL_D       = "http://test-r2.local/images/d.png";

    /** PNG 파일 시그니처(magic bytes) */
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private MockMultipartFile pngFile(final String name) {
        return new MockMultipartFile("file", name, "image/png", PNG_MAGIC);
    }

    /** DELETE /api/images 요청 body — {"urls": [...]} */
    private String cancelBody(final String... urls) throws Exception {
        return objectMapper.writeValueAsString(Map.of("urls", List.of(urls)));
    }

    @Nested
    @DisplayName("POST /api/images/upload — 이미지 업로드")
    class Upload {

        @Test
        @DisplayName("성공 — 유효한 X-Internal-Key와 PNG 파일 → 200 및 URL 반환")
        void success() throws Exception {
            given(imageService.upload(any())).willReturn(UPLOADED_IMAGE_URL);

            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(pngFile("test.png"))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.url").value(UPLOADED_IMAGE_URL));
        }

        @Test
        @DisplayName("실패 — X-Internal-Key 헤더 누락 → 403")
        void missingKey() throws Exception {
            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(pngFile("test.png")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 잘못된 X-Internal-Key → 403")
        void wrongKey() throws Exception {
            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(pngFile("test.png"))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, WRONG_KEY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 지원하지 않는 이미지 형식(gif) → 400")
        void invalidImageType() throws Exception {
            given(imageService.upload(any())).willThrow(new CustomException(ErrorCode.INVALID_IMAGE_TYPE));

            final MockMultipartFile gifFile = new MockMultipartFile(
                    "file", "test.gif", "image/gif", new byte[]{0x47, 0x49, 0x46}
            );

            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(gifFile)
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 파일 크기 10MB 초과 → 400")
        void fileTooLarge() throws Exception {
            given(imageService.upload(any())).willThrow(new CustomException(ErrorCode.IMAGE_TOO_LARGE));

            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(pngFile("big.png"))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — R2 업로드 오류 → 500")
        void r2UploadFailed() throws Exception {
            given(imageService.upload(any())).willThrow(new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED));

            mockMvc.perform(multipart(UPLOAD_URL)
                            .file(pngFile("test.png"))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/images — 미저장 이미지 취소")
    class CancelImages {

        @Test
        @DisplayName("성공 — 유효한 X-Internal-Key와 URL 목록 → 204")
        void success() throws Exception {
            willDoNothing().given(imageCleanupService).cancelImages(any());

            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody(IMAGE_URL_A))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("성공 — 최대 개수(3개) URL 목록 → 204")
        void successWithMaxUrls() throws Exception {
            willDoNothing().given(imageCleanupService).cancelImages(any());

            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody(IMAGE_URL_A, IMAGE_URL_B, IMAGE_URL_C))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 — X-Internal-Key 헤더 누락 → 403")
        void missingKey() throws Exception {
            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody(IMAGE_URL_A)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — 잘못된 X-Internal-Key → 403")
        void wrongKey() throws Exception {
            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody(IMAGE_URL_A))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, WRONG_KEY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — urls 빈 목록(@NotEmpty 위반) → 400")
        void emptyUrls() throws Exception {
            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody())
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — urls 최대 개수(3개) 초과(@Size 위반) → 400")
        void tooManyUrls() throws Exception {
            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody(IMAGE_URL_A, IMAGE_URL_B, IMAGE_URL_C, IMAGE_URL_D))
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 — urls 필드 누락(JSON 바디 없음) → 400")
        void missingUrlsField() throws Exception {
            mockMvc.perform(delete(CANCEL_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header(InternalApiConstants.INTERNAL_KEY_HEADER, VALID_KEY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
