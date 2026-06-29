package com.board.kimicetea.domain.comment.dto;

import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;

/**
 * 어드민 댓글 목록 응답 DTO — 숨겨진 댓글 포함, isVisible/postId 노출 (ADMIN 전용)
 */
public record AdminCommentSummaryResponse(
        Long id,
        Long postId,
        String postTypeCode,
        Long parentId,
        String content,
        String authorNickname,
        String authorMbtiId,
        String mbtiVerified,
        int likeCount,
        int dislikeCount,
        boolean isGuest,
        boolean isVisible,
        Instant createdAt
) {
    public static AdminCommentSummaryResponse from(final Comment comment) {
        final boolean hasUser = comment.getUser() != null;
        return new AdminCommentSummaryResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getPost().getPostType().name(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                hasUser ? comment.getUser().getNickname() : "(비회원)",
                comment.getAuthorMbti().getId(),
                hasUser ? comment.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                comment.getLikeCount(),
                comment.getDislikeCount(),
                comment.getGuestPassword() != null,
                comment.getIsVisible(),
                comment.getCreatedAt()
        );
    }
}
