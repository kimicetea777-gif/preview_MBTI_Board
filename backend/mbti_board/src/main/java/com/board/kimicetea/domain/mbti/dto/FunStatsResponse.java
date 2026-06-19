package com.board.kimicetea.domain.mbti.dto;

import java.util.List;

/**
 * FN 질문 통계 응답 DTO (GET /api/mbti/fun/stats)
 */
public record FunStatsResponse(
        Long questionId,
        String title,
        List<OptionStats> options
) {
    public record OptionStats(
            Long optionId,
            String optionText,
            int displayOrder,
            long totalCount,
            List<MbtiCount> mbtiCounts
    ) {}

    public record MbtiCount(
            String mbtiId,
            long count
    ) {}
}
