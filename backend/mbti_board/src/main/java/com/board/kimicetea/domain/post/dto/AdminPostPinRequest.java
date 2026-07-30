package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 어드민 게시글 고정/고정 해제 요청 DTO
 * 주의: ADMIN 전용 — SecurityConfig에서 /api/admin/** ROLE_ADMIN 보호
 */
public record AdminPostPinRequest(
        @NotNull(message = "pinned 필드는 필수입니다.")
        Boolean pinned
) {
}
