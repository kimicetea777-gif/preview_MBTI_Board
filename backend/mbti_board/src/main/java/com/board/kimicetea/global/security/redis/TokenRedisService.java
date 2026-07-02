package com.board.kimicetea.global.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 Refresh Token 저장/삭제 및 AT 블랙리스트 관리
 * 주의: deleteAllRefreshTokens()는 SCAN으로 구현 — 탈취 감지(RT Reuse Detection) 시에만 호출
 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String RT_PREFIX = "RT:";
    private static final String BL_PREFIX = "BL:";
    private static final String WD_PREFIX = "WD:";
    private static final String WD_CNT_PREFIX = "WD:CNT:";
    private static final String EV_TOKEN_PREFIX = "EV:TOKEN:";
    private static final String EV_EMAIL_PREFIX = "EV:EMAIL:";
    private static final String EV_DONE_PREFIX = "EV:DONE:";
    private static final String PW_RESET_PREFIX = "PW:RESET:";
    private static final String PW_RESET_EMAIL_PREFIX = "PW:RESET:EMAIL:";
    private static final String LOGOUT_VALUE = "logout";
    private static final long WITHDRAWAL_OTP_TTL_MS = 3 * 60 * 1000L; // 3분
    private static final long EMAIL_VERIFY_TOKEN_TTL_MS = 10 * 60 * 1000L; // 10분
    private static final long EMAIL_VERIFIED_TTL_MS = 30 * 60 * 1000L; // 30분
    private static final long PW_RESET_TOKEN_TTL_MS = 30 * 60 * 1000L; // 30분

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

    // ── 이메일 인증 토큰 ───────────────────────────────────────────

    /**
     * 인증 링크 토큰 저장 — Key: EV:TOKEN:{token}=email, EV:EMAIL:{email}=token, TTL: 10분
     * 주의: 재발급 시 기존 토큰을 먼저 무효화하여 동시에 유효한 토큰이 두 개 생기지 않도록 함
     */
    public void saveEmailVerifyToken(final String token, final String email) {
        final String oldToken = redisTemplate.opsForValue().get(evEmailKey(email));
        if (oldToken != null) {
            redisTemplate.delete(evTokenKey(oldToken));
        }
        redisTemplate.opsForValue().set(evTokenKey(token), email, EMAIL_VERIFY_TOKEN_TTL_MS, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(evEmailKey(email), token, EMAIL_VERIFY_TOKEN_TTL_MS, TimeUnit.MILLISECONDS);
    }

    /** 토큰으로 이메일 조회 — 만료 또는 미존재 시 empty 반환 */
    public Optional<String> getEmailByVerifyToken(final String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(evTokenKey(token)));
    }

    /** 토큰 사용 완료 후 양방향 키 모두 삭제 */
    public void deleteEmailVerifyToken(final String token) {
        final String email = redisTemplate.opsForValue().get(evTokenKey(token));
        redisTemplate.delete(evTokenKey(token));
        if (email != null) {
            redisTemplate.delete(evEmailKey(email));
        }
    }

    /*---------------------------------------------------- */
    /** 이메일 인증 완료 마킹 — Key: EV:DONE:{email}, TTL: 30분 (회원가입 완료 전까지 유효) */
    public void markEmailVerified(final String email) {
        redisTemplate.opsForValue().set(evDoneKey(email), "1", EMAIL_VERIFIED_TTL_MS, TimeUnit.MILLISECONDS);
    }

    public boolean isEmailVerified(final String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(evDoneKey(email)));
    }

    /** 회원가입 완료 후 인증 마킹 제거 */
    public void clearEmailVerified(final String email) {
        redisTemplate.delete(evDoneKey(email));
    }

    // ── 비밀번호 재설정 토큰 ──────────────────────────────────────

    /**
     * 비밀번호 재설정 토큰 저장 — PW:RESET:{token} + PW:RESET:EMAIL:{email} 쌍으로 저장, TTL: 30분
     * 주의: 재요청 시 기존 토큰을 먼저 deletePasswordResetToken()으로 무효화 후 호출
     */
    public void savePasswordResetToken(final String token, final String email) {
        redisTemplate.opsForValue().set(pwResetKey(token), email, PW_RESET_TOKEN_TTL_MS, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(pwResetEmailKey(email), token, PW_RESET_TOKEN_TTL_MS, TimeUnit.MILLISECONDS);
    }

    /** 토큰으로 이메일 조회 — 만료 또는 미존재 시 empty 반환 */
    public Optional<String> getEmailByPasswordResetToken(final String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(pwResetKey(token)));
    }

    /** 이메일로 기존 토큰 조회 — 재요청 시 기존 링크 무효화에 사용 */
    public String getPasswordResetTokenByEmail(final String email) {
        return redisTemplate.opsForValue().get(pwResetEmailKey(email));
    }

    /** 토큰·이메일 키 모두 삭제 — 토큰 사용 완료 또는 재요청 시 기존 토큰 무효화 */
    public void deletePasswordResetToken(final String token, final String email) {
        redisTemplate.delete(pwResetKey(token));
        redisTemplate.delete(pwResetEmailKey(email));
    }

    // ── 키 생성 헬퍼 ──────────────────────────────────────────────

    /** RT:{userId}:{deviceId} — 기기별 Refresh Token */
    private String rtKey(final String userId, final String deviceId) {
        return RT_PREFIX + userId + ":" + deviceId;
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

    /** EV:TOKEN:{token} — 회원가입 이메일 인증 토큰 → email */
    private String evTokenKey(final String token) {
        return EV_TOKEN_PREFIX + token;
    }

    /** EV:EMAIL:{email} — 이메일 인증 역방향 키 (email → token, 기존 토큰 무효화용) */
    private String evEmailKey(final String email) {
        return EV_EMAIL_PREFIX + email;
    }

    /** EV:DONE:{email} — 이메일 인증 완료 여부 (회원가입 전 인증 확인용) */
    private String evDoneKey(final String email) {
        return EV_DONE_PREFIX + email;
    }

    /** PW:RESET:{token} — 비밀번호 재설정 토큰 → email */
    private String pwResetKey(final String token) {
        return PW_RESET_PREFIX + token;
    }

    /** PW:RESET:EMAIL:{email} — 비밀번호 재설정 토큰 역방향 조회 (email → token, 중복 발급 방지) */
    private String pwResetEmailKey(final String email) {
        return PW_RESET_EMAIL_PREFIX + email;
    }
}
