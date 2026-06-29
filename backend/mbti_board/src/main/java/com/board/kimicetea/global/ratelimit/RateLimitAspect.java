package com.board.kimicetea.global.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @RateLimit 어노테이션이 붙은 메서드 진입 전 IP 기반 Rate Limit 체크
 * 주의: Spring MVC 요청 스레드에서만 동작 (RequestContextHolder 의존)
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Before("@annotation(rateLimit)")
    public void check(final RateLimit rateLimit) {
        final HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        rateLimitService.check(rateLimit.endpoint(), request, rateLimit.max(), rateLimit.windowSecs());
    }
}
