package com.board.kimicetea.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청 DTO
 * 주의: debateStance 유효성(1/-1 여부, 게시글 유형 일치)은 서비스 레이어에서 검증, 자유 게시물은 null
 */
public record CommentCreateRequest(
                @NotBlank @Size(max = 1000) String content,
                Long parentId,
                @Size(min = 4, max = 72) String guestPassword,
                Short debateStance) {
}
