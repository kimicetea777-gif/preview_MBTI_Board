package com.board.kimicetea.global.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token 저장/삭제 및 AT 블랙리스트 관리
 * 주의: deleteAllRefreshTokens()는 KEYS 명령을 사용하므로 탈취 감지 시에만 호출 (RT Reuse Detection)
 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String RT_PREFIX              = "RT:";
    private static final String BL_PREFIX              = "BL:";
    private static final String WD_PREFIX              = "WD:";
    private static final String WD_CNT_PREFIX          = "WD:CNT:";
    private static final String EV_TOKEN_PREFIX        = "EV:TOKEN:";
    private static final String EV_DONE_PREFIX         = "EV:DONE:";
    private static final String LOGOUT_VALUE           = "logout";
    private static final long   WITHDRAWAL_OTP_TTL_MS  = 3 * 60 * 1000L;   // 3분
    private static final long   EMAIL_VERIFY_TOKEN_TTL = 10 * 60 * 1000L;  // 10분
    private static final long   EMAIL_VERIFIED_TTL     = 30 * 60 * 1000L;  // 30분
    public static final int     WITHDRAWAL_OTP_MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    /** RT 저장 — Key: RT:{userId}:{deviceId} */
    public void saveRefreshToken(final String userId, final String deviceId,
                                 final String refreshToken, final long ttlMillis) {
        redisTemplate.opsForValue().set(
                rtKey(userId, deviceId),
                refreshToken,
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public String getRefreshToken(final String userId, final String deviceId) {
        return redisTemplate.opsForValue().get(rtKey(userId, deviceId));
    }

    /** 단일 기기 RT 삭제 */
    public void deleteRefreshToken(final String userId, final String deviceId) {
        redisTemplate.delete(rtKey(userId, deviceId));
    }

    /** RT Reuse Detection: 해당 userId의 모든 RT 즉시 폐기 → 전체 기기 강제 로그아웃 */
    public void deleteAllRefreshTokens(final String userId) {
        final Set<String> keys = redisTemplate.keys(RT_PREFIX + userId + ":*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    /** AT 블랙리스트 등록 — TTL: AT 남은 유효 시간 */
    public void addToBlacklist(final String accessToken, final long remainingMillis) {
        if (remainingMillis <= 0) return;
        redisTemplate.opsForValue().set(
                blKey(accessToken),
                LOGOUT_VALUE,
                remainingMillis,
                TimeUnit.MILLISECONDS
        );
    }

    public boolean isBlacklisted(final String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blKey(accessToken)));
    }

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
     * @return 누적 시도 횟수
     */
    public int incrementWithdrawalOtpAttempts(final String userId) {
        final Long count = redisTemplate.opsForValue().increment(wdCntKey(userId));
        if (count != null && count == 1L) {
            redisTemplate.expire(wdCntKey(userId), WITHDRAWAL_OTP_TTL_MS, TimeUnit.MILLISECONDS);
        }
        return count != null ? count.intValue() : 1;
    }

    /** OTP 및 시도 횟수 카운터 모두 삭제 */
    public void deleteWithdrawalOtp(final String userId) {
        redisTemplate.delete(wdKey(userId));
        redisTemplate.delete(wdCntKey(userId));
    }

    // ── 이메일 인증 토큰 ───────────────────────────────────────────

    /** 인증 링크 토큰 저장 — Key: EV:TOKEN:{token}, TTL: 10분 */
    public void saveEmailVerifyToken(final String token, final String email) {
        redisTemplate.opsForValue().set(evTokenKey(token), email, EMAIL_VERIFY_TOKEN_TTL, TimeUnit.MILLISECONDS);
    }

    /** 토큰으로 이메일 조회 — 만료 또는 미존재 시 null 반환 */
    public String getEmailByVerifyToken(final String token) {
        return redisTemplate.opsForValue().get(evTokenKey(token));
    }

    public void deleteEmailVerifyToken(final String token) {
        redisTemplate.delete(evTokenKey(token));
    }

    /** 인증 완료 마킹 — Key: EV:DONE:{email}, TTL: 30분 (회원가입 완료 전까지 유효) */
    public void markEmailVerified(final String email) {
        redisTemplate.opsForValue().set(evDoneKey(email), "1", EMAIL_VERIFIED_TTL, TimeUnit.MILLISECONDS);
    }

    public boolean isEmailVerified(final String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(evDoneKey(email)));
    }

    /** 회원가입 완료 후 인증 마킹 제거 */
    public void clearEmailVerified(final String email) {
        redisTemplate.delete(evDoneKey(email));
    }

    // ── 키 생성 헬퍼 ──────────────────────────────────────────────

    private String rtKey(final String userId, final String deviceId) {
        return RT_PREFIX + userId + ":" + deviceId;
    }

    private String blKey(final String accessToken) {
        return BL_PREFIX + accessToken;
    }

    private String wdKey(final String userId) {
        return WD_PREFIX + userId;
    }

    private String wdCntKey(final String userId) {
        return WD_CNT_PREFIX + userId;
    }

    private String evTokenKey(final String token) {
        return EV_TOKEN_PREFIX + token;
    }

    private String evDoneKey(final String email) {
        return EV_DONE_PREFIX + email;
    }
}
