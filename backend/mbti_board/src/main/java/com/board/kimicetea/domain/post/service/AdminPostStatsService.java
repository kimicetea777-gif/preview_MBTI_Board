package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.global.common.AdminStatsCachePolicy;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse.CountEntry;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.post.repository.PostRepository;
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
 * 어드민 게시글 통계 서비스 — 계산 결과를 Redis에 캐싱하여 HTTP 응답을 즉시 반환
 * 주의: computeAndCache()는 AdminPostStatsScheduler가 2시간마다 호출 — HTTP 요청 스레드에서 DB 집계 없음
 * 주의: Redis 미스(최초 기동·캐시 만료) 시 getCachedStats()가 직접 호출 — 드문 경우
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPostStatsService {

    private static final long TREND_DAYS = 30L;
    private static final String STATS_CACHE_KEY = "ADMIN:POST:STATS";

    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에서 캐시된 통계 반환 — 미스 시 computeAndCache() 직접 호출(자기 호출이므로 트랜잭션 미적용)
     * 주의: 미스는 최초 기동 또는 Redis 초기화 시에만 발생 — 정상 운영 중엔 캐시 히트
     */
    public AdminPostStatsResponse getCachedStats() {
        final String json = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, AdminPostStatsResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("게시글 통계 캐시 역직렬화 실패 — 재계산", e);
            }
        }
        return computeAndCache();
    }

    /**
     * 게시글 통계 계산 후 Redis에 저장 — AdminPostStatsScheduler(2시간) 및 캐시 미스 시 호출
     * 주의: 스케줄러에서 호출 시 Spring 프록시 경유 → @Transactional 정상 적용 (단일 커넥션)
     * 주의: TTL 4시간은 갱신 주기(2시간)의 2배 — 스케줄러 장애 시 안전망, 정상 운영 중엔 2시간마다 갱신됨
     */
    @Transactional(readOnly = true)
    public AdminPostStatsResponse computeAndCache() {
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

        final AdminPostStatsResponse stats = new AdminPostStatsResponse(
                totalPosts, hiddenCount, thisMonthNewPosts, lastMonthNewPosts,
                byType, byMbti, postTrend);

        try {
            redisTemplate.opsForValue().set(
                    STATS_CACHE_KEY,
                    objectMapper.writeValueAsString(stats),
                    AdminStatsCachePolicy.TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("게시글 통계 캐시 직렬화 실패", e);
        }

        return stats;
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
