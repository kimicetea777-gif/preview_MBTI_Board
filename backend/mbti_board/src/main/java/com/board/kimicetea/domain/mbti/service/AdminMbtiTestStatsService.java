package com.board.kimicetea.domain.mbti.service;

import com.board.kimicetea.global.common.AdminStatsCachePolicy;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse.CountEntry;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository.AvgScoreRow;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository.DateValueRow;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository.NameValueRow;
import com.board.kimicetea.domain.mbti.repository.MbtiTestHistoryRepository.RetakeBucketRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 어드민 MBTI 검사 통계 서비스 — 계산 결과를 Redis에 캐싱하여 HTTP 응답을 즉시 반환
 * 주의: computeAndCache()는 AdminMbtiTestStatsScheduler가 2시간마다 호출 — HTTP 요청 스레드에서 DB 집계 없음
 * 주의: Redis 미스(최초 기동·캐시 만료) 시 getCachedStats()가 직접 호출 — 드문 경우
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMbtiTestStatsService {

    private static final long TREND_DAYS = 30L;
    private static final String STATS_CACHE_KEY = "ADMIN:MBTI:STATS";

    private final MbtiTestHistoryRepository mbtiTestHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에서 캐시된 통계 반환 — 미스 시 computeAndCache() 직접 호출(자기 호출이므로 트랜잭션 미적용)
     * 주의: 미스는 최초 기동 또는 Redis 초기화 시에만 발생 — 정상 운영 중엔 캐시 히트
     */
    public AdminMbtiTestStatsResponse getCachedStats() {
        final String json = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, AdminMbtiTestStatsResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("MBTI 검사 통계 캐시 역직렬화 실패 — 재계산", e);
            }
        }
        return computeAndCache();
    }

    /**
     * MBTI 검사 통계 계산 후 Redis에 저장 — AdminMbtiTestStatsScheduler(2시간) 및 캐시 미스 시 호출
     * 주의: 스케줄러에서 호출 시 Spring 프록시 경유 → @Transactional 정상 적용 (단일 커넥션)
     * 주의: TTL 4시간은 갱신 주기(2시간)의 2배 — 스케줄러 장애 시 안전망, 정상 운영 중엔 2시간마다 갱신됨
     */
    @Transactional(readOnly = true)
    public AdminMbtiTestStatsResponse computeAndCache() {
        final long totalTests = mbtiTestHistoryRepository.count();

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthTests = mbtiTestHistoryRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthTests = mbtiTestHistoryRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);

        final List<AvgScoreRow> avgRows = mbtiTestHistoryRepository.avgScores();
        final AvgScoreRow avg = avgRows.isEmpty() ? null : avgRows.get(0);
        final double avgEiScore = avg == null || avg.getAvgEi() == null ? 0.0 : avg.getAvgEi();
        final double avgSnScore = avg == null || avg.getAvgSn() == null ? 0.0 : avg.getAvgSn();
        final double avgTfScore = avg == null || avg.getAvgTf() == null ? 0.0 : avg.getAvgTf();
        final double avgJpScore = avg == null || avg.getAvgJp() == null ? 0.0 : avg.getAvgJp();

        final List<CountEntry> byResult = toCountEntries(mbtiTestHistoryRepository.countGroupByResultRaw());

        final List<RetakeBucketRow> bucketRows = mbtiTestHistoryRepository.countRetakeBuckets();
        final RetakeBucketRow buckets = bucketRows.isEmpty() ? null : bucketRows.get(0);
        final List<CountEntry> byRetakeCount = List.of(
                new CountEntry("1회", buckets == null || buckets.getOnce() == null ? 0L : buckets.getOnce()),
                new CountEntry("2회", buckets == null || buckets.getTwice() == null ? 0L : buckets.getTwice()),
                new CountEntry("3회+", buckets == null || buckets.getThreeOrMore() == null ? 0L : buckets.getThreeOrMore())
        );

        final Instant since = Instant.now().minus(TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> testTrend = toDailyEntries(mbtiTestHistoryRepository.findTestTrendRaw(since));

        final AdminMbtiTestStatsResponse stats = new AdminMbtiTestStatsResponse(
                totalTests, thisMonthTests, lastMonthTests,
                avgEiScore, avgSnScore, avgTfScore, avgJpScore,
                byResult, byRetakeCount, testTrend);

        try {
            redisTemplate.opsForValue().set(
                    STATS_CACHE_KEY,
                    objectMapper.writeValueAsString(stats),
                    AdminStatsCachePolicy.TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("MBTI 검사 통계 캐시 직렬화 실패", e);
        }

        return stats;
    }

    private List<CountEntry> toCountEntries(final List<NameValueRow> rows) {
        return rows.stream()
                .map(row -> new CountEntry(
                        row.getName() == null ? "없음" : row.getName(),
                        row.getValue()))
                .toList();
    }

    private List<DailyCountEntry> toDailyEntries(final List<DateValueRow> rows) {
        return rows.stream()
                .map(row -> new DailyCountEntry(row.getDate(), row.getValue()))
                .toList();
    }
}
