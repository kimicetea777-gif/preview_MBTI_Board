package com.board.kimicetea.global.security.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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

    private static final String RT_PREFIX = "RT:";
    private static final String BL_PREFIX = "BL:";
    private static final String LOGOUT_VALUE = "logout";

    private final RedisTemplate<String, String> redisTemplate;

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

    private String rtKey(final String userId, final String deviceId) {
        return RT_PREFIX + userId + ":" + deviceId;
    }

    private String blKey(final String accessToken) {
        return BL_PREFIX + accessToken;
    }
}
