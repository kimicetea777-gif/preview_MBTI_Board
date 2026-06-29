package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;

import java.time.Instant;

/**
 * 마이페이지 내 게시글 프리뷰 응답 DTO — 최근 5개, 작성자 정보 제외
 */
public record MyPostPreviewResponse(
        Long id,
        String typeCode,
        String title,
        int commentCount,
        int viewCount,
        Instant createdAt
) {
    public static MyPostPreviewResponse from(final Post post) {
        return new MyPostPreviewResponse(
                post.getId(),
                post.getPostType().name(),
                post.getTitle(),
                post.getCommentCount(),
                post.getViewCount(),
                post.getCreatedAt()
        );
    }
}
