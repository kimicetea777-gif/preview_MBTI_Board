package com.board.kimicetea.domain.comment.dto;

/**
 * 댓글 리액션 응답 DTO — 리액션 조작 후 최신 카운트를 함께 반환
 * myReactionType: null이면 리액션 없음, 1 = LIKE, -1 = DISLIKE
 */
public record CommentReactionResponse(
        Long commentId,
        Short myReactionType,
        int likeCount,
        int dislikeCount
) {
}
