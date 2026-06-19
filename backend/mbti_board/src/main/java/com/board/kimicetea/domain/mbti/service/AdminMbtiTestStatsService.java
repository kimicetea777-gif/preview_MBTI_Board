package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse.CountEntry;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 어드민 MBTI 검사 통계 조회 서비스 — 결과 분포, 지표 평균, 재검사 빈도, 추이 집계
 * 주의: 모든 쿼리는 읽기 전용 트랜잭션으로 실행됨
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMbtiTestStatsService {

    private static final long TREND_DAYS = 30L;

    private final MbtiTestHistoryRepository mbtiTestHistoryRepository;

    /** MBTI 검사 전체 통계 집계 */
    public AdminMbtiTestStatsResponse getStats() {
        final long totalTests = mbtiTestHistoryRepository.count();

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthTests = mbtiTestHistoryRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthTests = mbtiTestHistoryRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);

        final List<Object[]> avgRows = mbtiTestHistoryRepository.avgScores();
        final Object[] avg = avgRows.isEmpty() ? new Object[]{null, null, null, null} : avgRows.get(0);
        final double avgEiScore = avg[0] == null ? 0.0 : ((Number) avg[0]).doubleValue();
        final double avgSnScore = avg[1] == null ? 0.0 : ((Number) avg[1]).doubleValue();
        final double avgTfScore = avg[2] == null ? 0.0 : ((Number) avg[2]).doubleValue();
        final double avgJpScore = avg[3] == null ? 0.0 : ((Number) avg[3]).doubleValue();

        final List<CountEntry> byResult = toCountEntries(mbtiTestHistoryRepository.countGroupByResultRaw());

        final List<Object[]> bucketRows = mbtiTestHistoryRepository.countRetakeBuckets();
        final Object[] buckets = bucketRows.isEmpty() ? new Object[]{null, null, null} : bucketRows.get(0);
        final List<CountEntry> byRetakeCount = List.of(
                new CountEntry("1회", buckets[0] == null ? 0L : ((Number) buckets[0]).longValue()),
                new CountEntry("2회", buckets[1] == null ? 0L : ((Number) buckets[1]).longValue()),
                new CountEntry("3회+", buckets[2] == null ? 0L : ((Number) buckets[2]).longValue())
        );

        final Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> testTrend = toDailyEntries(mbtiTestHistoryRepository.findTestTrendRaw(since));

        return new AdminMbtiTestStatsResponse(
                totalTests, thisMonthTests, lastMonthTests,
                avgEiScore, avgSnScore, avgTfScore, avgJpScore,
                byResult, byRetakeCount, testTrend);
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
