package com.board.kimicetea.global.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** IP 기반 Rate Limit — AOP가 메서드 진입 전 자동으로 check() 호출 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String endpoint();
    int max();
    int windowSecs();
}
