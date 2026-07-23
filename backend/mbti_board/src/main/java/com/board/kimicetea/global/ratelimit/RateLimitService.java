package com.board.kimicetea.global.ratelimit;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.common.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Redis 기반 Rate Limiting — Lua 스크립트로 INCR+EXPIRE 원자적 처리
 * 주의: Cloudflare Tunnel 환경이므로 X-Forwarded-For 헤더에서 실제 클라이언트 IP 추출
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RL_PREFIX = "RL:";

    // (Lua 스크립트) INCR + EXPIRE를 원자적으로 처리 — count == 1일 때만 TTL 설정, 현재 count 반환
    @NonNull
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n" +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return c",
            Long.class);

    @Value("${rate-limit.ip-salt}")
    private String ipSalt;

    private final StringRedisTemplate redisTemplate;

    /**
     * IP별 요청 횟수를 확인하고 한도 초과 시 예외 발생
     *
     * @param endpoint    Rate Limit 키 구분자 (예: "find-email")
     * @param request     클라이언트 IP 추출용
     * @param maxRequests 허용 횟수
     * @param windowSecs  시간 창(초)
     */
    public void check(final String endpoint, final HttpServletRequest request, final int maxRequests,
            final int windowSecs) {
        checkKey(RL_PREFIX + endpoint + ":" + RequestUtils.hashIp(ipSalt, RequestUtils.extractClientIp(request)), maxRequests, windowSecs);
    }

    /**
     * 이메일 주소별 요청 횟수를 확인하고 한도 초과 시 예외 발생
     * — IP 우회(프록시·VPN)를 통한 이메일 스팸 발송 방지
     *
     * @param endpoint    Rate Limit 키 구분자 (예: "email-verify")
     * @param email       대상 이메일 주소
     * @param maxRequests 허용 횟수
     * @param windowSecs  시간 창(초)
     */
    public void checkByEmail(final String endpoint, final String email,
            final int maxRequests, final int windowSecs) {
        // 플러스 주소(user+tag@domain) 우회 방지: 로컬 파트의 + 이후 제거 후 키 생성
        final int atIdx = email.indexOf('@');
        final String local = email.substring(0, atIdx);
        final String normalizedLocal = local.contains("+") ? local.substring(0, local.indexOf('+')) : local;
        final String normalizedEmail = normalizedLocal.toLowerCase() + email.substring(atIdx).toLowerCase();
        checkKey(RL_PREFIX + endpoint + ":m:" + normalizedEmail, maxRequests, windowSecs);
    }

    /*---------------------------------------------------------(private) Helper---------------------------------------------------------------*/
    private void checkKey(final String key, final int maxRequests, final int windowSecs) {
        final Long count = redisTemplate.execute(
                INCR_WITH_EXPIRE,
                Objects.requireNonNull(List.of(key)),
                String.valueOf(windowSecs));

        if (count > maxRequests) {
            throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

}
