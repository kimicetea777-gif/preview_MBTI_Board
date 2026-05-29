package com.board.kimicetea.global.image.service;

import com.board.kimicetea.global.image.entity.DeletedImage;
import com.board.kimicetea.global.image.entity.DeletedImageStatus;
import com.board.kimicetea.global.image.repository.DeletedImageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

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
    private final ObjectMapper objectMapper;

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
        if (urls.isEmpty()) return;
        deletedImageRepository.deleteByUrlsAndStatus(new ArrayList<>(urls), DeletedImageStatus.PENDING);
    }

    /**
     * 게시글 작성 취소 시 호출 — R2 즉시 삭제 시도, 실패 시 DELETE_REQUESTED로 전환
     * 주의: caller(ImageController) 트랜잭션에 합류
     */
    @Transactional
    public void cancelImages(final List<String> urls) {
        if (urls.isEmpty()) return;
        final List<DeletedImage> records = deletedImageRepository.findAllByUrlIn(urls);
        if (records.isEmpty()) return;

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
        final Set<String> removed = extractImageUrls(oldContent);
        if (removed.isEmpty()) return;
        removed.removeAll(extractImageUrls(newContent));
        if (removed.isEmpty()) return;

        deletedImageRepository.saveAll(
                removed.stream()
                        .map(DeletedImage::markedForCleanup)
                        .toList()
        );
    }

    /**
     * 매일 오전 3시
     * - PENDING > 24h : 게시글 저장 없이 방치된 이미지 삭제
     * - DELETE_REQUESTED : 즉시 삭제 실패 건 재시도
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        final Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        final List<DeletedImage> targets = new ArrayList<>();
        targets.addAll(deletedImageRepository.findAllByStatusAndMarkedAtBefore(DeletedImageStatus.PENDING, cutoff));
        targets.addAll(deletedImageRepository.findAllByStatus(DeletedImageStatus.DELETE_REQUESTED));

        if (targets.isEmpty()) return;

        int deleted = 0;
        for (final DeletedImage image : targets) {
            try {
                r2Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(keyFromUrl(image.getUrl()))
                        .build());
                deleted++;
            } catch (Exception e) {
                log.warn("R2 이미지 삭제 실패 — url={}", image.getUrl(), e);
            }
        }
        deletedImageRepository.deleteAll(targets);
        log.info("이미지 배치 정리 완료 — 삭제 성공: {}/{}", deleted, targets.size());
    }

    /** Tiptap JSON에서 자체 R2 이미지 URL만 추출 */
    public Set<String> extractImageUrls(final String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) return Set.of();
        try {
            final Set<String> urls = new HashSet<>();
            collectImageUrls(objectMapper.readTree(jsonContent), urls, 0);
            return urls;
        } catch (Exception e) {
            return Set.of();
        }
    }

    private void collectImageUrls(final JsonNode node, final Set<String> urls, final int depth) {
        if (depth > 20) return;
        if (node.isObject()) {
            if ("image".equals(node.path("type").asText())) {
                final String src = node.path("attrs").path("src").asText(null);
                // 자체 R2 URL 또는 {public-url}/ 플레이스홀더 이미지만 수집
                if (src != null && (src.startsWith(r2PublicUrl) || src.startsWith("{public-url}/"))) {
                    urls.add(src);
                }
            }
            if (node.has("content")) {
                collectImageUrls(node.path("content"), urls, depth + 1);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                collectImageUrls(child, urls, depth + 1);
            }
        }
    }

    private String keyFromUrl(final String url) {
        if (url.startsWith("{public-url}/")) {
            return url.substring("{public-url}/".length());
        }
        final String prefix = r2PublicUrl + "/";
        if (!url.startsWith(prefix)) {
            throw new IllegalArgumentException("R2 URL 형식이 올바르지 않음: " + url);
        }
        return url.substring(prefix.length());
    }
}
