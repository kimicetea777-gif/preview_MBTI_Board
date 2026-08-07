package com.board.kimicetea.global.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Redis 기반 탈퇴 OTP 저장·삭제 관리 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String WD_PREFIX = "WD:"; // 탈퇴 OTP: {userId} → 6자리 코드
    private static final String WD_CNT_PREFIX = "WD:CNT:"; // 탈퇴 OTP 오답 횟수: {userId} → 시도 횟수
    private static final long WITHDRAWAL_OTP_TTL_MS = 3 * 60 * 1000L; // 3분

    // INCR + EXPIRE 원자적 처리 — count == 1일 때만 TTL 설정 (RateLimitService와 동일 패턴)
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n" +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return c",
            Long.class);
    public static final int WITHDRAWAL_OTP_MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    /**
     * 탈퇴 OTP 저장 — Key: WD:{userId}, TTL: 3분
     * 주의: 재발급 시 시도 횟수 카운터도 함께 초기화
     */
    public void saveWithdrawalOtp(final String userId, final String otp) {
        redisTemplate.opsForValue().set(wdKey(userId), otp, WITHDRAWAL_OTP_TTL_MS, TimeUnit.MILLISECONDS);
        redisTemplate.delete(wdCntKey(userId));
    }

    public String getWithdrawalOtp(final String userId) {
        return redisTemplate.opsForValue().get(wdKey(userId));
    }

    /**
     * 잘못된 OTP 시도 횟수 증가 — Key: WD:CNT:{userId}, TTL: OTP와 동일
     * 
     * @return 누적 시도 횟수
     */
    public int incrementWithdrawalOtpAttempts(final String userId) {
        final Long count = redisTemplate.execute(
                INCR_WITH_EXPIRE,
                List.of(wdCntKey(userId)),
                String.valueOf(WITHDRAWAL_OTP_TTL_MS / 1000));
        return count != null ? count.intValue() : 1;
    }

    /** OTP 및 시도 횟수 카운터 모두 삭제 */
    public void deleteWithdrawalOtp(final String userId) {
        redisTemplate.delete(wdKey(userId));
        redisTemplate.delete(wdCntKey(userId));
    }

    // ── 키 생성 헬퍼 ──────────────────────────────────────────────

    /** WD:{userId} — 탈퇴 진행 중 OTP */
    private String wdKey(final String userId) {
        return WD_PREFIX + userId;
    }

    /** WD:CNT:{userId} — 탈퇴 OTP 실패 횟수 */
    private String wdCntKey(final String userId) {
        return WD_CNT_PREFIX + userId;
    }
}
