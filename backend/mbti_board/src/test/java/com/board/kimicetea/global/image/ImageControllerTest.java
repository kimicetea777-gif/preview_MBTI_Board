package com.board.kimicetea.global.image;

import com.board.kimicetea.IntegrationTestSupport;
import com.board.kimicetea.global.image.entity.DeletedImage;
import com.board.kimicetea.global.image.entity.DeletedImageStatus;
import com.board.kimicetea.global.image.repository.DeletedImageRepository;
import com.board.kimicetea.global.mail.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이미지 PENDING 등록·취소 API 통합 테스트
 * 주의: S3Client는 @MockBean — 실제 R2 호출 차단
 */
@AutoConfigureMockMvc
class ImageControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DeletedImageRepository deletedImageRepository;

    @MockBean EmailService emailService;
    @MockBean S3Client r2Client;

    private static final String VALID_URL    = "http://test-r2.local/images/550e8400-e29b-41d4-a716-446655440000.webp";
    private static final String EXTERNAL_URL = "https://evil.com/image.jpg";
    private static final String INTERNAL_KEY = "test-internal-api-key";
    private static final String WRONG_KEY    = "wrong-key";

    @AfterEach
    void tearDown() {
        deletedImageRepository.deleteAll();
    }

    // ─────────────────────────────────────────────────────────────
    // 1단계 — PENDING 등록 (POST /api/images/pending)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-1. X-Internal-Key 헤더 없음 → 403")
    void PENDING_등록_내부키_없음_실패() throws Exception {
        mockMvc.perform(post(IMAGE_PENDING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("url", VALID_URL))))
                .andExpect(status().isForbidden());

        assertThat(deletedImageRepository.count()).isZero();
    }

    @Test
    @DisplayName("1-2. 잘못된 X-Internal-Key → 403")
    void PENDING_등록_잘못된_내부키_실패() throws Exception {
        mockMvc.perform(post(IMAGE_PENDING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Key", WRONG_KEY)
                        .content(objectMapper.writeValueAsString(Map.of("url", VALID_URL))))
                .andExpect(status().isForbidden());

        assertThat(deletedImageRepository.count()).isZero();
    }

    @Test
    @DisplayName("1-3. 유효한 키 + 자체 R2 URL → 200, DB에 PENDING 레코드 생성")
    void PENDING_등록_성공() throws Exception {
        mockMvc.perform(post(IMAGE_PENDING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .content(objectMapper.writeValueAsString(Map.of("url", VALID_URL))))
                .andExpect(status().isOk());

        final List<DeletedImage> saved = deletedImageRepository.findAllByUrlIn(List.of(VALID_URL));
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStatus()).isEqualTo(DeletedImageStatus.PENDING);
    }

    @Test
    @DisplayName("1-4. 유효한 키 + 외부 URL → 200, DB 저장 안 됨 (URL 검증 통과 후 서비스에서 무시)")
    void PENDING_등록_외부URL_무시() throws Exception {
        mockMvc.perform(post(IMAGE_PENDING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .content(objectMapper.writeValueAsString(Map.of("url", EXTERNAL_URL))))
                .andExpect(status().isOk());

        assertThat(deletedImageRepository.findAllByUrlIn(List.of(EXTERNAL_URL))).isEmpty();
    }

    @Test
    @DisplayName("1-5. url 빈 값 → 400 (Bean Validation)")
    void PENDING_등록_빈URL_실패() throws Exception {
        mockMvc.perform(post(IMAGE_PENDING_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Key", INTERNAL_KEY)
                        .content(objectMapper.writeValueAsString(Map.of("url", ""))))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // 2단계 — 이미지 취소 (DELETE /api/images)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2-1. urls 빈 리스트 → 400 (Bean Validation @NotEmpty)")
    void 이미지_취소_빈목록_실패() throws Exception {
        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2-2. urls 21개 → 400 (Bean Validation @Size(max=20))")
    void 이미지_취소_목록_최대초과_실패() throws Exception {
        final List<String> oversized = IntStream.range(0, 21)
                .mapToObj(i -> "http://test-r2.local/images/" + i + ".webp")
                .toList();

        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", oversized))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("2-3. PENDING 레코드 없는 URL → 204, DB 변화 없음 (no-op)")
    void 이미지_취소_PENDING없는URL_성공() throws Exception {
        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of(VALID_URL)))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("2-4. PENDING 레코드 있는 URL + R2 삭제 성공 → 204, DB 레코드 제거됨")
    void 이미지_취소_PENDING있는URL_성공() throws Exception {
        deletedImageRepository.save(DeletedImage.pending(VALID_URL));

        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of(VALID_URL)))))
                .andExpect(status().isNoContent());

        assertThat(deletedImageRepository.findAllByUrlIn(List.of(VALID_URL))).isEmpty();
    }

    @Test
    @DisplayName("2-5. PENDING 레코드 있는 URL + R2 삭제 실패 → 204, 레코드가 DELETE_REQUESTED로 전환")
    void 이미지_취소_R2삭제실패_DELETE_REQUESTED_전환() throws Exception {
        deletedImageRepository.save(DeletedImage.pending(VALID_URL));

        org.mockito.BDDMockito.given(r2Client.deleteObject(
                org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .willThrow(new RuntimeException("R2 연결 실패"));

        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of(VALID_URL)))))
                .andExpect(status().isNoContent());

        final List<DeletedImage> remaining = deletedImageRepository.findAllByUrlIn(List.of(VALID_URL));
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getStatus()).isEqualTo(DeletedImageStatus.DELETE_REQUESTED);
    }
}
