package com.board.kimicetea.domain.post.dto;

import java.util.List;

/** GET /api/admin/posts/stats 응답 — 게시글 현황·타입 분포·MBTI별 작성 분포·작성 추이 통계 */
public record AdminPostStatsResponse(
        long totalPosts,
        long hiddenCount,
        long thisMonthNewPosts,
        long lastMonthNewPosts,
        List<CountEntry> byType,
        List<CountEntry> byMbti,
        List<DailyCountEntry> postTrend
) {
    /** 이름(타입·MBTI 등)과 카운트 쌍 */
    public record CountEntry(String name, long value) {}

    /** 날짜(MM-DD)와 카운트 쌍 — 작성 추이 */
    public record DailyCountEntry(String date, long value) {}
}
