package com.board.kimicetea.domain.user.service;

import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.CountEntry;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.DailyCountEntry;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse.RoleTrendEntry;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.repository.WithdrawalLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 어드민 유저 통계 조회 서비스 — 상태·역할·MBTI 분포, 가입 추이, 장기 미접속 집계
 * 주의: 모든 쿼리는 읽기 전용 트랜잭션으로 실행됨
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserStatsService {

    private static final long INACTIVE_DAYS = 90L;
    private static final long SIGNUP_TREND_DAYS = 30L;

    private final UserRepository userRepository;
    private final WithdrawalLogRepository withdrawalLogRepository;

    /**
     * 유저 전체 통계 집계 — 한 번의 호출로 대시보드에 필요한 데이터를 모두 반환
     */
    public AdminUserStatsResponse getStats() {
        final long totalUsers = userRepository.count();
        final long socialLoginCount = userRepository.countSocialLogin();
        final long localLoginCount = totalUsers - socialLoginCount;

        final Instant inactiveCutoff = Instant.now().minus(INACTIVE_DAYS, ChronoUnit.DAYS);
        final long inactiveCount = userRepository.countByStateAndLastLoginAtBefore(
                UserState.NORMAL, inactiveCutoff);

        // 이번 달 초 기준 90일 전 → 당시 이미 미접속이었던 수 (차이 = 이번 달 신규 장기미접속)
        final ZonedDateTime nowForInactive = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStartForInactive = nowForInactive.withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthInactiveCutoff = thisMonthStartForInactive.minus(INACTIVE_DAYS, ChronoUnit.DAYS);
        final long lastMonthInactiveCount = userRepository.countByStateAndLastLoginAtBefore(
                UserState.NORMAL, lastMonthInactiveCutoff);

        final List<CountEntry> byState = toCountEntries(userRepository.countGroupByStateRaw());
        final List<CountEntry> byRole = toCountEntries(userRepository.countGroupByRoleRaw());
        final List<CountEntry> byMbti = toCountEntries(userRepository.countGroupByMbtiRaw());
        final List<CountEntry> byMbtiVerified = toCountEntries(userRepository.countGroupByMbtiVerifiedRaw());

        final Instant signupSince = Instant.now().minus(SIGNUP_TREND_DAYS, ChronoUnit.DAYS);
        final List<DailyCountEntry> signupTrend = toDailyEntries(userRepository.findSignupTrendRaw(signupSince));
        final List<DailyCountEntry> withdrawalTrend = toDailyEntries(withdrawalLogRepository.findTrendRaw(signupSince));

        final List<RoleTrendEntry> roleTrend = toRoleTrendEntries(
                userRepository.findRoleTrendRaw(signupSince));

        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final Instant thisMonthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final Instant lastMonthStart = now.minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
        final long thisMonthNewUsers = userRepository.countByCreatedAtBetween(thisMonthStart, Instant.now());
        final long lastMonthNewUsers = userRepository.countByCreatedAtBetween(lastMonthStart, thisMonthStart);

        final long phoneVerifiedCount = userRepository.countHasPhone();
        final long longPendingCount = userRepository.countByStateAndCreatedAtBefore(
                UserState.PENDING, Instant.now().minus(7, ChronoUnit.DAYS));

        return new AdminUserStatsResponse(
                totalUsers, inactiveCount, lastMonthInactiveCount,
                socialLoginCount, localLoginCount,
                thisMonthNewUsers, lastMonthNewUsers,
                phoneVerifiedCount, longPendingCount,
                byState, byRole, byMbti, byMbtiVerified, signupTrend, withdrawalTrend, roleTrend);
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
