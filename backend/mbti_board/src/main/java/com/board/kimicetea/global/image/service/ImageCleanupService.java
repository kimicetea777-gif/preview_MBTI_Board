package com.board.kimicetea.global.image.service;

import com.board.kimicetea.global.common.util.TiptapContentParser;
import com.board.kimicetea.global.image.ImageConstants;
import com.board.kimicetea.global.image.entity.DeletedImage;
import com.board.kimicetea.global.image.entity.DeletedImageStatus;
import com.board.kimicetea.global.image.repository.DeletedImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * R2 이미지 삭제 예약·배치 정리 서비스
 * 주의: markRemovedImages()는 트랜잭션 없이 호출 시 caller의 트랜잭션에 합류
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupService {

    private final DeletedImageRepository deletedImageRepository;
    private final S3Client r2Client;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url}")
    private String r2PublicUrl;

    /**
     * 업로드 직후 PENDING 레코드 등록 — Next.js 업로드 Route Handler가 R2 업로드 후 호출
     * 주의: 자체 R2 URL이 아닌 경우 무시 (내부 키 유출 시 외부 URL 등록 방어)
     */
    @Transactional
    public void registerPending(final String url) {
        if (!url.startsWith(r2PublicUrl + "/")) {
            log.warn("PENDING 등록 거부 — 자체 R2 URL 아님: {}", url);
            return;
        }
        deletedImageRepository.save(DeletedImage.pending(url));
    }

    /**
     * 게시글 저장 성공 시 호출 — 본문에 포함된 이미지의 PENDING 레코드 제거
     * 주의: caller(PostService) 트랜잭션 안에서 실행되어야 롤백이 함께 적용됨
     */
    public void commitImages(final Set<String> urls) {
        if (urls.isEmpty())
            return;
        deletedImageRepository.deleteByUrlsAndStatus(new ArrayList<>(urls), DeletedImageStatus.PENDING);
    }

    /**
     * 게시글 작성 취소 시 호출 — R2 즉시 삭제 시도, 실패 시 DELETE_REQUESTED로 전환
     * 주의: caller(ImageController) 트랜잭션에 합류
     */
    @Transactional
    public void cancelImages(final List<String> urls) {
        if (urls.isEmpty())
            return;
        final List<DeletedImage> records = deletedImageRepository.findAllByUrlIn(urls);
        if (records.isEmpty())
            return;

        final List<DeletedImage> deleted = new ArrayList<>();
        for (final DeletedImage record : records) {
            try {
                r2Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(keyFromUrl(record.getUrl()))
                        .build());
                deleted.add(record);
            } catch (Exception e) {
                log.warn("이미지 즉시 삭제 실패, DELETE_REQUESTED 전환 — url={}", record.getUrl(), e);
                record.markDeleteRequested();
            }
        }
        if (!deleted.isEmpty()) {
            deletedImageRepository.deleteAll(deleted);
        }
    }

    /**
     * 게시글 수정 시 content diff — 이전 content에만 있던 이미지 URL을 삭제 예약
     * 주의: caller(PostService)의 트랜잭션 안에서 실행되어야 롤백이 함께 적용됨
     */
    public void markRemovedImages(final String oldContent, final String newContent) {
        final Set<String> removed = TiptapContentParser.extractImageUrls(oldContent, r2PublicUrl);
        if (removed.isEmpty())
            return;
        removed.removeAll(TiptapContentParser.extractImageUrls(newContent, r2PublicUrl));
        if (removed.isEmpty())
            return;

        deletedImageRepository.saveAll(
                removed.stream()
                        .map(DeletedImage::markedForCleanup)
                        .toList());
    }

    /*--------------------------------------------cleanup()-------------------------------------------------- */
    /**
     * 매일 오전 3시
     * - PENDING > 24h : 게시글 저장 없이 방치된 이미지 삭제
     * - DELETE_REQUESTED : 즉시 삭제 실패 건 재시도
     * 주의: R2 삭제 성공한 건만 DB에서 제거 — 실패 시 다음 날 재시도
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        final List<DeletedImage> targets = collectCleanupTargets();
        if (targets.isEmpty())
            return;

        final Set<String> failedKeys = deleteFromR2(targets);

        final List<DeletedImage> succeeded = targets.stream()
                .filter(image -> !failedKeys.contains(keyFromUrl(image.getUrl())))
                .toList();

        if (!succeeded.isEmpty())
            deletedImageRepository.deleteAll(succeeded);
        log.info("이미지 배치 정리 완료 — 삭제 성공: {}/{}", succeeded.size(), targets.size());
    }

    private List<DeletedImage> collectCleanupTargets() {
        final Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        final List<DeletedImage> targets = new ArrayList<>();
        targets.addAll(deletedImageRepository.findAllByStatusAndMarkedAtBefore(DeletedImageStatus.PENDING, cutoff));
        targets.addAll(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED));
        return targets;
    }

    private Set<String> deleteFromR2(final List<DeletedImage> targets) {
        final Set<String> failedKeys = new HashSet<>();
        for (int i = 0; i < targets.size(); i += ImageConstants.DELETE_OBJECTS_BATCH_SIZE) {
            final List<DeletedImage> chunk = targets.subList(i,
                    Math.min(i + ImageConstants.DELETE_OBJECTS_BATCH_SIZE, targets.size()));
            deleteChunk(chunk, failedKeys);
        }
        return failedKeys;
    }

    private void deleteChunk(final List<DeletedImage> chunk, final Set<String> failedKeys) {
        final List<ObjectIdentifier> identifiers = chunk.stream()
                .map(image -> ObjectIdentifier.builder().key(keyFromUrl(image.getUrl())).build())
                .toList();
        try {
            final DeleteObjectsResponse response = r2Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(identifiers).build())
                            .build());

            response.errors().forEach(error -> {
                log.warn("R2 이미지 삭제 실패 — key={}, code={}", error.key(), error.code());
                failedKeys.add(error.key());
            });
        } catch (Exception e) {
            log.warn("R2 배치 삭제 요청 실패 — chunk size={}", chunk.size(), e);
            chunk.forEach(image -> failedKeys.add(keyFromUrl(image.getUrl())));
        }
    }

    private String keyFromUrl(final String url) {
        if (url.startsWith(TiptapContentParser.PLACEHOLDER_PREFIX)) {
            return url.substring(TiptapContentParser.PLACEHOLDER_PREFIX.length());
        }
        final String prefix = r2PublicUrl + "/";
        if (!url.startsWith(prefix)) {
            throw new IllegalArgumentException("R2 URL 형식이 올바르지 않음: " + url);
        }
        return url.substring(prefix.length());
    }
}
