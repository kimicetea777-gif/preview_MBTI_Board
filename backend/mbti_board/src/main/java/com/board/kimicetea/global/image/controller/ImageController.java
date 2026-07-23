package com.board.kimicetea.global.image.controller;

import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.image.ImageConstants;
import com.board.kimicetea.global.image.dto.ImageCancelRequest;
import com.board.kimicetea.global.image.dto.ImageUploadResponse;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.board.kimicetea.global.image.service.ImageService;
import com.board.kimicetea.global.internal.InternalOnly;
import com.board.kimicetea.global.ratelimit.RateLimit;
import com.board.kimicetea.global.ratelimit.RateLimitEndpoints;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드·취소 API 컨트롤러
 * 주의: /upload·DELETE 모두 X-Internal-Key 헤더로 보호 (Next.js 서버 전용)
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final ImageCleanupService imageCleanupService;

    @Operation(summary = "이미지 업로드 (내부 API)", description = "Next.js Route Handler가 Sharp 변환 후 호출. " +
            "X-Internal-Key 헤더 필수. WebP·jpg·png·bmp, 최대 10MB. " +
            "업로드 성공 시 PENDING 등록 — 24h 내 게시글 미저장 시 자동 삭제.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내부 키 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Rate Limit 초과")
    })
    @InternalOnly
    @RateLimit(endpoint = RateLimitEndpoints.UPLOAD, max = ImageConstants.RATE_LIMIT_MAX, windowSecs = ImageConstants.RATE_LIMIT_WINDOW_SECONDS)
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> upload(@RequestParam final MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(new ImageUploadResponse(imageService.upload(file))));
    }

    @Operation(summary = "미저장 이미지 취소", description = "게시글 작성 취소 시 업로드된 이미지 URL 목록을 전달. " +
            "X-Internal-Key 헤더 필수. PENDING 레코드에 한해 R2 즉시 삭제 시도. 실패 시 DELETE_REQUESTED로 전환하여 스케줄러 재시도.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내부 키 불일치")
    })
    @InternalOnly
    @DeleteMapping
    public ResponseEntity<Void> cancelImages(@RequestBody @Valid final ImageCancelRequest request) {
        imageCleanupService.cancelImages(request.urls());
        return ResponseEntity.noContent().build();
    }
}
