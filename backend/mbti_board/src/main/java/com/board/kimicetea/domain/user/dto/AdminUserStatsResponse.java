package com.board.kimicetea.domain.user.dto;

import java.util.List;

/** GET /api/admin/users/stats 응답 — 유저 현황·분포·가입·탈퇴 추이 통계 */
public record AdminUserStatsResponse(
        long totalUsers,
        long inactiveCount,
        long lastMonthInactiveCount,
        long thisMonthNewUsers,
        long lastMonthNewUsers,
        long longPendingCount,
        List<CountEntry> byState,
        List<CountEntry> byRole,
        List<CountEntry> byMbti,
        List<CountEntry> byMbtiVerified,
        List<DailyCountEntry> signupTrend,
        List<DailyCountEntry> withdrawalTrend,
        List<RoleTrendEntry> roleTrend
) {
    /** 이름(상태·역할·MBTI 등)과 카운트 쌍 */
    public record CountEntry(String name, long value) {}

    /** 날짜(MM-DD)와 카운트 쌍 — 가입 추이 */
    public record DailyCountEntry(String date, long value) {}

    /** 날짜별 ADMIN·TESTER 신규 생성 수 */
    public record RoleTrendEntry(String date, long adminCount, long testerCount) {}
}
