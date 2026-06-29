package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;

/**
 * 어드민 게시글 목록 응답 DTO — 숨겨진 게시글 포함, isVisible/typeCode 노출 (ADMIN 전용)
 */
public record AdminPostSummaryResponse(
        Long id,
        String typeCode,
        String title,
        String authorNickname,
        String authorMbtiId,
        String mbtiVerified,
        String targetMbtiId,
        int viewCount,
        int commentCount,
        boolean isVisible,
        Instant createdAt
) {
    public static AdminPostSummaryResponse from(final Post post) {
        final boolean hasUser = post.getUser() != null;
        return new AdminPostSummaryResponse(
                post.getId(),
                post.getPostType().name(),
                post.getTitle(),
                hasUser ? post.getUser().getNickname() : "(비회원)",
                post.getAuthorMbti().getId(),
                hasUser ? post.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                post.getTargetMbti() != null ? post.getTargetMbti().getId() : null,
                post.getViewCount(),
                post.getCommentCount(),
                post.getIsVisible(),
                post.getCreatedAt()
        );
    }
}
