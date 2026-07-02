package com.board.kimicetea.global.aop;

import com.board.kimicetea.global.alert.AlertMarkers;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Repository 메서드 실행 시간 감시 — 임계치 초과 시 DB_QUERY_SLOW 알림 발송
 * 주의: JPA 레이어 전체 시간(캐시 포함) 기준이며 실제 SQL 시간과 차이가 있을 수 있음
 */
@Slf4j
@Aspect
@Component
public class SlowQueryAspect {

    @Value("${alert.slow-query.threshold-ms:1000}")
    private long thresholdMs;

    @Around("execution(* com.board.kimicetea..repository..*(..))")
    public Object checkSlowQuery(final ProceedingJoinPoint pjp) throws Throwable {
        final long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            final long elapsed = System.currentTimeMillis() - start;
            if (elapsed > thresholdMs) {
                log.error(AlertMarkers.DB_QUERY_SLOW,
                        "Slow query detected: {} ({}ms, threshold: {}ms)",
                        pjp.getSignature().toShortString(), elapsed, thresholdMs);
            }
        }
    }
}
