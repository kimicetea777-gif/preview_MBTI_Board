package com.board.kimicetea.global.ratelimit;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 IP별 Rate Limiting — INCR + EXPIRE 방식
 * 주의: Cloudflare Tunnel 환경이므로 X-Forwarded-For 헤더에서 실제 클라이언트 IP 추출
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RL_PREFIX = "RL:";

    private final StringRedisTemplate redisTemplate;

    /**
     * IP별 요청 횟수를 확인하고 한도 초과 시 예외 발생
     *
     * @param endpoint    Rate Limit 키 구분자 (예: "find-email")
     * @param request     클라이언트 IP 추출용
     * @param maxRequests 허용 횟수
     * @param windowSecs  시간 창(초)
     */
    public void check(final String endpoint, final HttpServletRequest request,
            final int maxRequests, final int windowSecs) {
        final String ip = extractClientIp(request);
        final String key = RL_PREFIX + endpoint + ":" + ip;

        final Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (count == 1) {
            redisTemplate.expire(key, windowSecs, TimeUnit.SECONDS);
        }
        if (count > maxRequests) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    /** Cloudflare Tunnel 환경: X-Forwarded-For 우선, 없으면 RemoteAddr */
    private String extractClientIp(final HttpServletRequest request) {
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
