package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;

/**
 * 게시글 목록 응답 DTO — content 제외, 렌더링 최소화
 */
public record PostSummaryResponse(
        Long id,
        String title,
        String authorNickname,
        String authorMbtiId,
        String authorMbtiColor,
        String mbtiVerified,
        String mbtiVerifiedColor,
        String targetMbtiId,
        int viewCount,
        int commentCount,
        Instant createdAt
) {
    public static PostSummaryResponse from(final Post post) {
        final boolean hasUser = post.getUser() != null;
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                hasUser ? post.getUser().getNickname() : "(비회원)",
                post.getAuthorMbti().getId(),
                post.getAuthorMbti().getColorCode(),
                hasUser ? post.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                hasUser ? post.getUser().getMbtiVerified().getColorCode() : MbtiVerified.DEFAULT.getColorCode(),
                post.getTargetMbti() != null ? post.getTargetMbti().getId() : null,
                post.getViewCount(),
                post.getCommentCount(),
                post.getCreatedAt()
        );
    }
}
