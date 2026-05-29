package com.board.kimicetea.global.image.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PENDING 이미지 등록 요청 DTO — Next.js 업로드 Route Handler가 R2 업로드 후 호출
 * 주의: X-Internal-Key 헤더가 함께 필요하며, 백엔드 내부 API 전용
 */
public record ImagePendingRequest(@NotBlank String url) {}
