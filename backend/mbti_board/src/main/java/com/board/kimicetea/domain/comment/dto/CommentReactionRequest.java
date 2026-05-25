package com.board.kimicetea.domain.comment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 댓글 리액션 요청 DTO
 * 주의: reactionType 유효성(1/-1 여부)은 서비스 레이어에서 검증
 */
public record CommentReactionRequest(
        @NotNull Short reactionType  // 1 = LIKE, -1 = DISLIKE
) {
}
