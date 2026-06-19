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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이미지 업로드·취소 API 통합 테스트
 * 주의: S3Client는 @MockBean — 실제 R2 호출 차단
 */
@AutoConfigureMockMvc
class ImageControllerTest extends IntegrationTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DeletedImageRepository deletedImageRepository;
    @Autowired StringRedisTemplate stringRedisTemplate;

    @MockBean EmailService emailService;
    @MockBean S3Client r2Client;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private static final String VALID_URL = "http://test-r2.local/images/550e8400-e29b-41d4-a716-446655440000.webp";

    // RIFF....WEBP — magic bytes 검증을 통과하는 최소 WebP 파일 (12 bytes)
    private static final byte[] MINIMAL_WEBP = {
            0x52, 0x49, 0x46, 0x46,  // "RIFF"
            0x0C, 0x00, 0x00, 0x00,  // 파일 크기 (little-endian, 12)
            0x57, 0x45, 0x42, 0x50,  // "WEBP"
    };

    @AfterEach
    void tearDown() {
        deletedImageRepository.deleteAll();
        // Rate Limit 키 초기화 — MockMvc RemoteAddr은 127.0.0.1
        stringRedisTemplate.delete("rate_limit:upload:127.0.0.1");
    }

    /* ======================== 업로드 테스트 (2-x) ======================== */

    @Test
    @DisplayName("2-1. X-Internal-Key 헤더 없음 → 403")
    void 업로드_키없음_403() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", MINIMAL_WEBP);

        mockMvc.perform(multipart(IMAGE_UPLOAD_URL).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2-2. X-Internal-Key 헤더 틀림 → 403")
    void 업로드_틀린키_403() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", MINIMAL_WEBP);

        mockMvc.perform(multipart(IMAGE_UPLOAD_URL).file(file)
                        .header("X-Internal-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2-3. 올바른 키 + 정상 WebP → 200, PENDING 레코드 생성됨")
    void 업로드_올바른키_200() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", MINIMAL_WEBP);

        mockMvc.perform(multipart(IMAGE_UPLOAD_URL).file(file)
                        .header("X-Internal-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").isString());

        final List<DeletedImage> pending = deletedImageRepository.findAll();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo(DeletedImageStatus.PENDING);
    }

    /* ======================== 취소 테스트 (1-x) ======================== */

    @Test
    @DisplayName("1-1. urls 빈 리스트 → 400 (Bean Validation @NotEmpty)")
    void 이미지_취소_빈목록_실패() throws Exception {
        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("1-2. urls 21개 → 400 (Bean Validation @Size(max=20))")
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
    @DisplayName("1-3. PENDING 레코드 없는 URL → 204, DB 변화 없음 (no-op)")
    void 이미지_취소_PENDING없는URL_성공() throws Exception {
        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of(VALID_URL)))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("1-4. PENDING 레코드 있는 URL + R2 삭제 성공 → 204, DB 레코드 제거됨")
    void 이미지_취소_PENDING있는URL_성공() throws Exception {
        deletedImageRepository.save(DeletedImage.pending(VALID_URL));

        mockMvc.perform(delete(IMAGE_CANCEL_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("urls", List.of(VALID_URL)))))
                .andExpect(status().isNoContent());

        assertThat(deletedImageRepository.findAllByUrlIn(List.of(VALID_URL))).isEmpty();
    }

    @Test
    @DisplayName("1-5. PENDING 레코드 있는 URL + R2 삭제 실패 → 204, 레코드가 DELETE_REQUESTED로 전환")
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
