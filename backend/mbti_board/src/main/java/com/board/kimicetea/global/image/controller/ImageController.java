package com.board.kimicetea.global.image.controller;

import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.image.dto.ImageCancelRequest;
import com.board.kimicetea.global.image.dto.ImagePendingRequest;
import com.board.kimicetea.global.image.dto.ImageUploadResponse;
import com.board.kimicetea.global.image.service.ImageCleanupService;
import com.board.kimicetea.global.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

/**
 * 이미지 업로드·PENDING 등록·취소 API 컨트롤러
 * 주의: /pending은 X-Internal-Key 헤더로 보호 (Next.js 서버 → 백엔드 내부 호출 전용)
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final ImageCleanupService imageCleanupService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Operation(summary = "이미지 업로드",
            description = "게시글 에디터에서 이미지를 업로드하고 공개 URL을 반환합니다. " +
                    "회원/비회원 모두 가능. jpg·png·gif·webp, 최대 5MB.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            @RequestParam final MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(new ImageUploadResponse(imageService.upload(file))));
    }

    @Operation(summary = "이미지 PENDING 등록 (내부 API)",
            description = "Next.js 업로드 Route Handler가 R2 업로드 성공 후 호출. " +
                    "X-Internal-Key 헤더 필수. 미저장 이미지를 24h 후 자동 정리 대상으로 등록.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "내부 키 불일치")
    })
    @PostMapping("/pending")
    public ResponseEntity<Void> registerPending(
            @RequestHeader(value = "X-Internal-Key", required = false) final String key,
            @RequestBody @Valid final ImagePendingRequest request) {
        // required=false: 헤더 부재 시 null → 403 반환 (MissingRequestHeaderException 방지)
        if (key == null || !internalApiKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        imageCleanupService.registerPending(request.url());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "미저장 이미지 취소",
            description = "게시글 작성 취소 시 업로드된 이미지 URL 목록을 전달. " +
                    "PENDING 레코드에 한해 R2 즉시 삭제 시도. 실패 시 DELETE_REQUESTED로 전환하여 스케줄러 재시도.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "처리 완료")
    @DeleteMapping
    public ResponseEntity<Void> cancelImages(@RequestBody @Valid final ImageCancelRequest request) {
        imageCleanupService.cancelImages(request.urls());
        return ResponseEntity.noContent().build();
    }
}
