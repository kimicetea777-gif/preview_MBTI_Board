package com.board.kimicetea.domain.user.dto;

import java.time.Instant;

/** 내 활동 통계 응답 DTO (게시글 수·댓글 수·최근 검사 MBTI·검사 일시) */
public record UserActivityStatsResponse(
        long postCount,
        long commentCount,
        String latestTestMbtiId,
        Instant latestTestDate
) {
    public static UserActivityStatsResponse of(long postCount, long commentCount, String latestTestMbtiId, Instant latestTestDate) {
        return new UserActivityStatsResponse(postCount, commentCount, latestTestMbtiId, latestTestDate);
    }
}
