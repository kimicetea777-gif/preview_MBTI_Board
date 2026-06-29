package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 게시글 목록 응답 DTO — contentPreview·imageUrls는 DB 저장값 사용 (JSONB 런타임 파싱 없음)
 * debateSummary/voteOptions는 각 게시글 유형일 때만 non-null
 */
public record PostSummaryResponse(
        Long id,
        String typeCode,
        String title,
        String contentPreview,
        List<String> imageUrls,
        String authorNickname,
        String authorMbtiId,
        String mbtiVerified,
        String targetMbtiId,
        int viewCount,
        int commentCount,
        Instant createdAt,
        DebateSummaryResponse debateSummary,
        List<VoteOptionResponse> voteOptions
) {
    public static PostSummaryResponse from(final Post post, final String r2PublicUrl) {
        final boolean hasUser = post.getUser() != null;
        final String[] imageKeys = post.getImageUrls();
        final List<String> imageUrls = (imageKeys != null && imageKeys.length > 0)
                ? Arrays.stream(imageKeys).map(key -> r2PublicUrl + "/" + key).toList()
                : null;

        final DebateSummaryResponse debateSummary = post.getDebatePost() != null
                ? DebateSummaryResponse.from(post.getDebatePost())
                : null;

        final List<VoteOptionResponse> voteOptions = post.getVotePost() != null
                ? post.getVotePost().getOptions().stream()
                        .limit(3)
                        .map(VoteOptionResponse::from)
                        .toList()
                : null;

        return new PostSummaryResponse(
                post.getId(),
                post.getPostType().name(),
                post.getTitle(),
                post.getContentPreview(),
                imageUrls,
                hasUser ? post.getUser().getNickname() : "(비회원)",
                post.getAuthorMbti().getId(),
                hasUser ? post.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                post.getTargetMbti() != null ? post.getTargetMbti().getId() : null,
                post.getViewCount(),
                post.getCommentCount(),
                post.getCreatedAt(),
                debateSummary,
                voteOptions
        );
    }
}
