package com.board.kimicetea.domain.post.dto;

import java.util.List;

/**
 * 투표 선택지별 MBTI 분포 응답 DTO (GET /api/posts/{postId}/vote/mbti-stats)
 */
public record VoteMbtiStatsResponse(
        List<OptionMbtiStats> options
) {
    public record OptionMbtiStats(
            Long optionId,
            List<MbtiCount> mbtiCounts
    ) {}

    public record MbtiCount(
            String mbtiId,
            long count
    ) {}
}
