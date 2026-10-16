package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.global.common.AdminStatsCachePolicy;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.CountEntry;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.RoleTrendEntry;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.WithdrawalLogRepository;
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
 * 어드민 유저 통계 서비스 — 계산 결과를 Redis에 캐싱하여 HTTP 응답을 즉시 반환
 * 주의: computeAndCache()는 AdminUserStatsScheduler가 2시간마다 호출 — HTTP 요청 스레드에서 DB 집계 없음
 * 주의: Redis 미스(최초 기동·캐시 만료) 시 getCachedStats()가 직접 호출 — 드문 경우
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserStatsService {

    private static final long INACTIVE_DAYS = 90L;
    private static final long SIGNUP_TREND_DAYS = 30L;
    private static final String STATS_CACHE_KEY = "ADMIN:USER:STATS";

    private final UserRepository userRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis에서 캐시된 통계 반환 — 미스 시 computeAndCache() 직접 호출(자기 호출이므로 트랜잭션 미적용)
     * 주의: 미스는 최초 기동 또는 Redis 초기화 시에만 발생 — 정상 운영 중엔 캐시 히트
     */
    public AdminUserStatsResponse getCachedStats() {
        final String json = redisTemplate.opsForValue().get(STATS_CACHE_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, AdminUserStatsResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("통계 캐시 역직렬화 실패 — 재계산", e);
            }
        }
        return computeAndCache();
    }

    /**
     * 유저 통계 계산 후 Redis에 저장 — AdminUserStatsScheduler(2시간) 및 캐시 미스 시 호출
     * 주의: 스케줄러에서 호출 시 Spring 프록시 경유 → @Transactional 정상 적용 (단일 커넥션)
     * 주의: TTL 4시간은 갱신 주기(2시간)의 2배 — 스케줄러 장애 시 안전망, 정상 운영 중엔 2시간마다 갱신됨
     */
    @Transactional(readOnly = true)
    public AdminUserStatsResponse computeAndCache() {
        final Instant inactiveCutoff = Instant.now().minus(INACTIVE_DAYS, ChronoUnit.DAYS);
        final long inactiveCount = userRepository.countByStateAndLastLoginAtBefore(
                UserState.NORMAL, inactiveCutoff);

        final ZonedDateTime nowForInactive = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStartForInactive = nowForInactive.withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthInactiveCutoff = thisMonthStartForInactive.minus(INACTIVE_DAYS, ChronoUnit.DAYS);
        final long lastMonthInactiveCount = userRepository.countByStateAndLastLoginAtBefore(
                UserState.NORMAL, lastMonthInactiveCutoff);

        final List<CountEntry> byState = toCountEntries(userRepository.countGroupByStateRaw());
        final long totalUsers = byState.stream().mapToLong(CountEntry::value).sum();

        final List<CountEntry> byRole         = toCountEntries(userRepository.countGroupByRoleRaw());
        final List<CountEntry> byMbti         = toCountEntries(userRepository.countGroupByMbtiRaw());
        final List<CountEntry> byMbtiVerified = toCountEntries(userRepository.countGroupByMbtiVerifiedRaw());

        final Instant signupSince = Instant.now().minus(SIGNUP_TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> signupTrend     = toDailyEntries(userRepository.findSignupTrendRaw(signupSince));
        final List<DailyCountEntry> withdrawalTrend = toDailyEntries(withdrawalLogRepository.findTrendRaw(signupSince));
        final List<RoleTrendEntry>  roleTrend       = toRoleTrendEntries(userRepository.findRoleTrendRaw(signupSince));

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthNewUsers  = userRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthNewUsers  = userRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);
        final long longPendingCount   = userRepository.countByStateAndCreatedAtBefore(
                UserState.PENDING, Instant.now().minus(7, ChronoUnit.DAYS));

        final AdminUserStatsResponse stats = new AdminUserStatsResponse(
                totalUsers, inactiveCount, lastMonthInactiveCount,
                thisMonthNewUsers, lastMonthNewUsers,
                longPendingCount,
                byState, byRole, byMbti, byMbtiVerified, signupTrend, withdrawalTrend, roleTrend);

        try {
            redisTemplate.opsForValue().set(
                    STATS_CACHE_KEY,
                    objectMapper.writeValueAsString(stats),
                    AdminStatsCachePolicy.TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("통계 캐시 직렬화 실패", e);
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

    private List<RoleTrendEntry> toRoleTrendEntries(final List<Object[]> rows) {
        return rows.stream()
                .map(row -> new RoleTrendEntry(
                        row[0].toString(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .toList();
    }
}
