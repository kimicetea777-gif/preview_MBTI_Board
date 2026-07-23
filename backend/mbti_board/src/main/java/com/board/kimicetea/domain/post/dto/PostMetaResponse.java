package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;

import java.util.Arrays;
import java.util.List;

/**
 * 게시글 메타데이터 전용 응답 DTO — SEO/OG 미리보기 생성 전용
 * 주의: 조회수 증가·인증 여부와 무관 — GET /api/posts/{id}와 별개 경로로 관리
 */
public record PostMetaResponse(
        String title,
        String contentPreview,
        List<String> imageUrls) {

    public static PostMetaResponse from(final Post post, final String r2PublicUrl) {
        final String[] imageKeys = post.getImageUrls();
        final List<String> imageUrls = (imageKeys != null && imageKeys.length > 0)
                ? Arrays.stream(imageKeys).map(key -> r2PublicUrl + "/" + key).toList()
                : null;

        return new PostMetaResponse(post.getTitle(), post.getContentPreview(), imageUrls);
    }
}
