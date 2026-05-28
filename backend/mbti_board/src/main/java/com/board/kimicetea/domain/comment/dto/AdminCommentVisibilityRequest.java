package com.board.kimicetea.domain.comment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 어드민 댓글 숨김/복구 요청 DTO
 * 주의: ADMIN 전용 — SecurityConfig에서 /api/admin/** ROLE_ADMIN 보호
 */
public record AdminCommentVisibilityRequest(
        @NotNull(message = "visible 필드는 필수입니다.")
        Boolean visible
) {
}
