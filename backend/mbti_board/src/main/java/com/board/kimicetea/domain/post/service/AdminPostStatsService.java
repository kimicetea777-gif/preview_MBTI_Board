package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse.CountEntry;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 어드민 게시글 통계 조회 서비스 — 타입·MBTI 분포, 작성 추이, 숨김 현황 집계
 * 주의: 모든 쿼리는 읽기 전용 트랜잭션으로 실행됨
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostStatsService {

    private static final long TREND_DAYS = 30L;

    private final PostRepository postRepository;

    /** 게시글 전체 통계 집계 */
    public AdminPostStatsResponse getStats() {
        final long totalPosts = postRepository.count();
        final long hiddenCount = postRepository.countHidden();

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthNewPosts = postRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthNewPosts = postRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);

        final List<CountEntry> byType = toCountEntries(postRepository.countGroupByTypeRaw());
        final List<CountEntry> byMbti = toCountEntries(postRepository.countGroupByMbtiRaw());

        final Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> postTrend = toDailyEntries(postRepository.findPostTrendRaw(since));

        return new AdminPostStatsResponse(
                totalPosts, hiddenCount, thisMonthNewPosts, lastMonthNewPosts,
                byType, byMbti, postTrend);
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
