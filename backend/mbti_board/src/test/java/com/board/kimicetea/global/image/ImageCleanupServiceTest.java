package com.board.kimicetea.global.image;

import com.board.kimicetea.global.image.entity.DeletedImage;
import com.board.kimicetea.global.image.entity.DeletedImageStatus;
import com.board.kimicetea.global.image.repository.DeletedImageRepository;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * ImageCleanupService 단위 테스트
 * 주의: S3Client·Repository는 Mock — R2·DB 연동 없이 비즈니스 로직만 검증
 */
@ExtendWith(MockitoExtension.class)
class ImageCleanupServiceTest {

    @Mock DeletedImageRepository deletedImageRepository;
    @Mock S3Client r2Client;

    private ImageCleanupService imageCleanupService;

    private static final String R2_PUBLIC_URL = "http://test-r2.local";
    private static final String BUCKET        = "test-bucket";
    private static final String VALID_URL     = R2_PUBLIC_URL + "/images/550e8400-e29b-41d4-a716-446655440000.webp";
    private static final String EXTERNAL_URL  = "https://evil.com/image.jpg";

    @BeforeEach
    void setUp() {
        // ObjectMapper는 실제 객체 사용 — JSON 파싱 로직 실제 실행 필요
        imageCleanupService = new ImageCleanupService(deletedImageRepository, r2Client, new ObjectMapper());
        ReflectionTestUtils.setField(imageCleanupService, "r2PublicUrl", R2_PUBLIC_URL);
        ReflectionTestUtils.setField(imageCleanupService, "bucket", BUCKET);
    }

    // ─────────────────────────────────────────────────────────────
    // 1단계 — registerPending
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1-1. 자체 R2 URL → DB에 PENDING 상태로 저장")
    void registerPending_유효한_R2_URL_저장() {
        imageCleanupService.registerPending(VALID_URL);

        final ArgumentCaptor<DeletedImage> captor = ArgumentCaptor.forClass(DeletedImage.class);
        then(deletedImageRepository).should(times(1)).save(captor.capture());

        final DeletedImage saved = captor.getValue();
        assertThat(saved.getUrl()).isEqualTo(VALID_URL);
        assertThat(saved.getStatus()).isEqualTo(DeletedImageStatus.PENDING);
    }

    @Test
    @DisplayName("1-2. 외부 URL → DB 저장 없이 무시")
    void registerPending_외부URL_무시() {
        imageCleanupService.registerPending(EXTERNAL_URL);

        then(deletedImageRepository).should(never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // 2단계 — commitImages
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("2-1. 빈 URL 집합 → DB 조회·삭제 없음")
    void commitImages_빈_집합_no_op() {
        imageCleanupService.commitImages(Set.of());

        then(deletedImageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("2-2. URL 집합 전달 → PENDING 레코드 삭제 쿼리 호출")
    void commitImages_PENDING_레코드_삭제() {
        imageCleanupService.commitImages(Set.of(VALID_URL));

        then(deletedImageRepository).should(times(1))
                .deleteByUrlsAndStatus(List.of(VALID_URL), DeletedImageStatus.PENDING);
    }

    // ─────────────────────────────────────────────────────────────
    // 3단계 — cancelImages
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3-1. 빈 URL 목록 → DB 조회 없음")
    void cancelImages_빈_목록_no_op() {
        imageCleanupService.cancelImages(List.of());

        then(deletedImageRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("3-2. DB에 PENDING 레코드 없는 URL → R2 호출 없음")
    void cancelImages_PENDING_없는_URL_no_op() {
        given(deletedImageRepository.findAllByUrlIn(any())).willReturn(List.of());

        imageCleanupService.cancelImages(List.of(VALID_URL));

        then(r2Client).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("3-3. R2 삭제 성공 → DB 레코드 삭제")
    void cancelImages_R2삭제_성공_DB레코드_삭제() {
        final DeletedImage record = DeletedImage.pending(VALID_URL);
        given(deletedImageRepository.findAllByUrlIn(List.of(VALID_URL))).willReturn(List.of(record));

        imageCleanupService.cancelImages(List.of(VALID_URL));

        then(r2Client).should(times(1)).deleteObject(any(DeleteObjectRequest.class));
        then(deletedImageRepository).should(times(1)).deleteAll(List.of(record));
    }

    @Test
    @DisplayName("3-4. R2 삭제 실패 → DB 레코드가 DELETE_REQUESTED로 전환, deleteAll 미호출")
    void cancelImages_R2삭제_실패_DELETE_REQUESTED_전환() {
        final DeletedImage record = DeletedImage.pending(VALID_URL);
        given(deletedImageRepository.findAllByUrlIn(List.of(VALID_URL))).willReturn(List.of(record));
        given(r2Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(new RuntimeException("R2 연결 실패"));

        imageCleanupService.cancelImages(List.of(VALID_URL));

        assertThat(record.getStatus()).isEqualTo(DeletedImageStatus.DELETE_REQUESTED);
        then(deletedImageRepository).should(never()).deleteAll(any(List.class));
    }

    // ─────────────────────────────────────────────────────────────
    // 4단계 — cleanup (스케줄러)
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("4-1. 대상 없음 → R2·DB 호출 없음")
    void cleanup_대상없음_no_op() {
        given(deletedImageRepository.findAllByStatusAndMarkedAtBefore(any(), any())).willReturn(List.of());
        given(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED)).willReturn(List.of());

        imageCleanupService.cleanup();

        then(r2Client).shouldHaveNoInteractions();
        then(deletedImageRepository).should(never()).deleteAll(any(List.class));
    }

    @Test
    @DisplayName("4-2. PENDING 24h 초과 → R2 삭제 후 DB 레코드 삭제")
    void cleanup_PENDING_24시간_초과_삭제() {
        final DeletedImage stale = DeletedImage.pending(VALID_URL);
        given(deletedImageRepository.findAllByStatusAndMarkedAtBefore(
                eq(DeletedImageStatus.PENDING), any(Instant.class))).willReturn(List.of(stale));
        given(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED)).willReturn(List.of());

        imageCleanupService.cleanup();

        then(r2Client).should(times(1)).deleteObject(any(DeleteObjectRequest.class));
        then(deletedImageRepository).should(times(1)).deleteAll(List.of(stale));
    }

    @Test
    @DisplayName("4-3. DELETE_REQUESTED 레코드 → R2 재시도 후 DB 레코드 삭제")
    void cleanup_DELETE_REQUESTED_재시도_삭제() {
        final DeletedImage failed = DeletedImage.pending(VALID_URL);
        failed.markDeleteRequested();

        given(deletedImageRepository.findAllByStatusAndMarkedAtBefore(
                eq(DeletedImageStatus.PENDING), any(Instant.class))).willReturn(List.of());
        given(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED))
                .willReturn(List.of(failed));

        imageCleanupService.cleanup();

        then(r2Client).should(times(1)).deleteObject(any(DeleteObjectRequest.class));
        then(deletedImageRepository).should(times(1)).deleteAll(List.of(failed));
    }

    @Test
    @DisplayName("4-4. R2 삭제 실패해도 DB 레코드는 삭제됨 (grace period 종료 후 강제 정리)")
    void cleanup_R2삭제_실패해도_DB레코드_삭제() {
        final DeletedImage stale = DeletedImage.pending(VALID_URL);
        given(deletedImageRepository.findAllByStatusAndMarkedAtBefore(
                eq(DeletedImageStatus.PENDING), any(Instant.class))).willReturn(List.of(stale));
        given(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED)).willReturn(List.of());
        given(r2Client.deleteObject(any(DeleteObjectRequest.class)))
                .willThrow(new RuntimeException("R2 연결 실패"));

        imageCleanupService.cleanup();

        // R2 실패해도 DB는 반드시 정리
        then(deletedImageRepository).should(times(1)).deleteAll(List.of(stale));
    }
}
