package com.board.kimicetea.global.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token/AS ID Token 저장·삭제 및 AT 블랙리스트 관리
 * 주의: deleteAllRefreshTokens()는 SCAN으로 구현 — 탈취 감지(RT Reuse Detection) 시에만 호출
 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String RT_PREFIX = "RT:"; // Refresh Token: {userId}:{deviceId} → RT 값
    private static final String ID_PREFIX = "ID:"; // AS ID Token: {userId}:{deviceId} → id_token (RP-Initiated Logout용)
    private static final String BL_PREFIX = "BL:"; // AT 블랙리스트: {jti} → "logout"
    private static final String WD_PREFIX = "WD:"; // 탈퇴 OTP: {userId} → 6자리 코드
    private static final String WD_CNT_PREFIX = "WD:CNT:"; // 탈퇴 OTP 오답 횟수: {userId} → 시도 횟수
    private static final String LOGOUT_VALUE = "logout";
    private static final long WITHDRAWAL_OTP_TTL_MS = 3 * 60 * 1000L; // 3분

    // INCR + EXPIRE 원자적 처리 — count == 1일 때만 TTL 설정 (RateLimitService와 동일 패턴)
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n" +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return c",
            Long.class);
    public static final int WITHDRAWAL_OTP_MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    /** RT 저장 — Key: RT:{userId}:{deviceId} */
    public void saveRefreshToken(final String userId, final String deviceId,
            final String refreshToken, final long ttlMillis) {
        redisTemplate.opsForValue().set(
                rtKey(userId, deviceId),
                refreshToken,
                ttlMillis,
                TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(final String userId, final String deviceId) {
        return redisTemplate.opsForValue().get(rtKey(userId, deviceId));
    }

    /** 단일 기기 RT 삭제 */
    public void deleteRefreshToken(final String userId, final String deviceId) {
        redisTemplate.delete(rtKey(userId, deviceId));
    }

    /** AS ID Token 저장 — Key: ID:{userId}:{deviceId}. RP-Initiated Logout(/connect/logout)의 id_token_hint로만 씀 */
    public void saveIdToken(final String userId, final String deviceId, final String idToken, final long ttlMillis) {
        redisTemplate.opsForValue().set(idKey(userId, deviceId), idToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public String getIdToken(final String userId, final String deviceId) {
        return redisTemplate.opsForValue().get(idKey(userId, deviceId));
    }

    public void deleteIdToken(final String userId, final String deviceId) {
        redisTemplate.delete(idKey(userId, deviceId));
    }

    /** 전체 기기 로그아웃 시, 로그아웃 URL을 만들어준 현재 기기 것 말고 남아있는 다른 기기의 ID Token까지 정리 */
    public void deleteAllIdTokens(final String userId) {
        final String pattern = ID_PREFIX + userId + ":*";
        final List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** RT Reuse Detection: 해당 userId의 모든 RT 즉시 폐기 → 전체 기기 강제 로그아웃 */
    public void deleteAllRefreshTokens(final String userId) {
        final String pattern = RT_PREFIX + userId + ":*";
        final List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(100).build())) {
            cursor.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** AT 블랙리스트 등록 — Key: BL:{jti}, TTL: AT 남은 유효 시간 */
    public void addToBlacklist(final String jti, final long remainingMillis) {
        if (remainingMillis <= 0)
            return;
        redisTemplate.opsForValue().set(
                blKey(jti),
                LOGOUT_VALUE,
                remainingMillis,
                TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(final String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blKey(jti)));
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

    /** RT:{userId}:{deviceId} — 기기별 Refresh Token */
    private String rtKey(final String userId, final String deviceId) {
        return RT_PREFIX + userId + ":" + deviceId;
    }

    /** ID:{userId}:{deviceId} — 기기별 AS ID Token */
    private String idKey(final String userId, final String deviceId) {
        return ID_PREFIX + userId + ":" + deviceId;
    }

    /** BL:{accessToken} — 로그아웃된 Access Token 블랙리스트 */
    private String blKey(final String accessToken) {
        return BL_PREFIX + accessToken;
    }

    /** WD:{userId} — 탈퇴 진행 중 OTP */
    private String wdKey(final String userId) {
        return WD_PREFIX + userId;
    }

    /** WD:CNT:{userId} — 탈퇴 OTP 실패 횟수 */
    private String wdCntKey(final String userId) {
        return WD_CNT_PREFIX + userId;
    }
}
