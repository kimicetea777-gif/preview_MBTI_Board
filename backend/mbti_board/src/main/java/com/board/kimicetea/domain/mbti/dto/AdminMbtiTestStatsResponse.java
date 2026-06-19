package com.board.kimicetea.domain.mbti.dto;

import java.util.List;

/** GET /api/admin/mbti/stats 응답 — MBTI 검사 횟수·결과 분포·지표 평균·재검사 빈도·추이 통계 */
public record AdminMbtiTestStatsResponse(
        long totalTests,
        long thisMonthTests,
        long lastMonthTests,
        double avgEiScore,
        double avgSnScore,
        double avgTfScore,
        double avgJpScore,
        List<CountEntry> byResult,
        List<CountEntry> byRetakeCount,
        List<DailyCountEntry> testTrend
) {
    /** 이름(MBTI 타입·재검사 구간 등)과 카운트 쌍 */
    public record CountEntry(String name, long value) {}

    /** 날짜(MM-DD)와 카운트 쌍 — 검사 추이 */
    public record DailyCountEntry(String date, long value) {}
}
