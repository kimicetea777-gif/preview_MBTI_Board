package com.board.kimicetea.domain.comment.dto;

import java.util.List;

/** GET /api/admin/comments/stats 응답 — 댓글 현황·MBTI 분포·리액션 현황·작성 추이 통계 */
public record AdminCommentStatsResponse(
        long totalComments,
        long hiddenCount,
        long thisMonthNewComments,
        long lastMonthNewComments,
        long totalLikes,
        long totalDislikes,
        List<CountEntry> byMbti,
        List<DailyCountEntry> commentTrend
) {
    /** 이름(MBTI 등)과 카운트 쌍 */
    public record CountEntry(String name, long value) {}

    /** 날짜(MM-DD)와 카운트 쌍 — 작성 추이 */
    public record DailyCountEntry(String date, long value) {}
}
