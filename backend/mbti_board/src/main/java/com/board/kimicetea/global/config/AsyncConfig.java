package com.board.kimicetea.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;


/**
 * @Async 활성화 — 메일 발송 비동기 처리
 * 주의: spring.threads.virtual.enabled=true 설정으로 @Async Executor도 가상 스레드를 사용
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("비동기 처리 실패 [{}]: {}", method.getName(), ex.getMessage(), ex);
    }
}
