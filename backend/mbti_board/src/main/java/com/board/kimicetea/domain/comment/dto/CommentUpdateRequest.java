package com.board.kimicetea.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 요청 DTO
 * 주의: guestPassword는 비회원 전용 — 서비스에서 회원/비회원 분기 처리
 */
public record CommentUpdateRequest(
        @NotBlank @Size(max = 1000) String content,
        @Size(min = 4, max = 72) String guestPassword
) {
}
