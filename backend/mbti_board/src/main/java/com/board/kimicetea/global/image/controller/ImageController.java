package com.board.kimicetea.global.image.controller;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.image.dto.ImageCancelRequest;
import com.board.kimicetea.global.image.dto.ImageUploadResponse;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.board.kimicetea.global.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * 이미지 업로드·취소 API 컨트롤러
 * 주의: /upload·DELETE 모두 X-Internal-Key 헤더로 보호 (Next.js 서버 전용)
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private static final int RATE_LIMIT_MAX = 10;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    private final ImageService imageService;
    private final ImageCleanupService imageCleanupService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Operation(summary = "이미지 업로드 (내부 API)",
            description = "Next.js Route Handler가 Sharp 변환 후 호출. " +
                    "X-Internal-Key 헤더 필수. WebP·jpg·png·bmp, 최대 10MB. " +
                    "업로드 성공 시 PENDING 등록 — 24h 내 게시글 미저장 시 자동 삭제.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내부 키 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate Limit 초과")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> upload(
            @RequestHeader(value = "X-Internal-Key", required = false) final String key,
            @RequestParam final MultipartFile file,
            final HttpServletRequest request) {
        if (key == null || !internalApiKey.equals(key)) {
            throw new CustomException(ErrorCode.INTERNAL_KEY_INVALID);
        }

        String ip = request.getHeader("X-Real-IP");
        if (ip == null) {
            final String forwarded = request.getHeader("X-Forwarded-For");
            ip = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        }
        final String rateLimitKey = "rate_limit:upload:" + ip;
        final Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (Long.valueOf(1).equals(count)) {
            stringRedisTemplate.expire(rateLimitKey, Duration.ofSeconds(RATE_LIMIT_WINDOW_SECONDS));
        }
        if (count != null && count > RATE_LIMIT_MAX) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        return ResponseEntity.ok(ApiResponse.ok(new ImageUploadResponse(imageService.upload(file))));
    }

    @Operation(summary = "미저장 이미지 취소",
            description = "게시글 작성 취소 시 업로드된 이미지 URL 목록을 전달. " +
                    "X-Internal-Key 헤더 필수. PENDING 레코드에 한해 R2 즉시 삭제 시도. 실패 시 DELETE_REQUESTED로 전환하여 스케줄러 재시도.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내부 키 불일치")
    })
    @DeleteMapping
    public ResponseEntity<Void> cancelImages(
            @RequestHeader(value = "X-Internal-Key", required = false) final String key,
            @RequestBody @Valid final ImageCancelRequest request) {
        if (key == null || !internalApiKey.equals(key)) {
            throw new CustomException(ErrorCode.INTERNAL_KEY_INVALID);
        }
        imageCleanupService.cancelImages(request.urls());
        return ResponseEntity.noContent().build();
    }
}
