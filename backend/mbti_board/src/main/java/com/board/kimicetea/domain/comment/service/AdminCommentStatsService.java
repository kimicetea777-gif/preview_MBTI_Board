package com.board.kimicetea.domain.comment.service;

import com.board.kimicetea.domain.comment.dto.AdminCommentStatsResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentStatsResponse.CountEntry;
import com.board.kimicetea.domain.comment.dto.AdminCommentStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 어드민 댓글 통계 조회 서비스 — MBTI 분포, 리액션 현황, 작성 추이, 숨김 현황 집계
 * 주의: 모든 쿼리는 읽기 전용 트랜잭션으로 실행됨
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentStatsService {

    private static final long TREND_DAYS = 30L;

    private final CommentRepository commentRepository;

    /** 댓글 전체 통계 집계 */
    public AdminCommentStatsResponse getStats() {
        final long totalComments = commentRepository.count();
        final long hiddenCount = commentRepository.countHidden();

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthNewComments = commentRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthNewComments = commentRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);

        final List<Object[]> reactionRows = commentRepository.sumReactions();
        final Object[] reactionRow = reactionRows.isEmpty() ? new Object[]{null, null} : reactionRows.get(0);
        final long totalLikes    = reactionRow[0] == null ? 0L : ((Number) reactionRow[0]).longValue();
        final long totalDislikes = reactionRow[1] == null ? 0L : ((Number) reactionRow[1]).longValue();

        final List<CountEntry> byMbti = toCountEntries(commentRepository.countGroupByMbtiRaw());

        final Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> commentTrend = toDailyEntries(commentRepository.findCommentTrendRaw(since));

        return new AdminCommentStatsResponse(
                totalComments, hiddenCount, thisMonthNewComments, lastMonthNewComments,
                totalLikes, totalDislikes, byMbti, commentTrend);
    }

    private List<CountEntry> toCountEntries(final List<Object[]> rows) {
        return rows.stream()
                .map(row -> new CountEntry(
                        row[0] == null ? "없음" : row[0].toString(),
                        ((Number) row[1]).longValue()))
                .toList();
    }

    private List<DailyCountEntry> toDailyEntries(final List<Object[]> rows) {
        return rows.stream()
                .map(row -> new DailyCountEntry(
                        row[0].toString(),
                        ((Number) row[1]).longValue()))
                .toList();
    }
}
