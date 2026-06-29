package com.board.kimicetea.global.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Next.js 서버 등 내부 호출 전용 엔드포인트 — X-Internal-Key 헤더 검증을 AOP가 자동 처리 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalOnly {
}
