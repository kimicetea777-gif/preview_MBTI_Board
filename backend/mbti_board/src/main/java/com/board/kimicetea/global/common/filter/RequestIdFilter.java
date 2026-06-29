package com.board.kimicetea.global.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 고유한 requestId를 MDC에 심어 로그 추적을 가능하게 하는 필터
 * 주의: HIGHEST_PRECEDENCE로 Security 필터보다 먼저 실행되어야 JWT 인증 로그에도 requestId가 찍힘
 * 주의: 반드시 finally에서 MDC를 제거해야 스레드 재사용 시 오염 방지됨
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements Filter {

    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString().substring(0, 8));
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_KEY);
        }
    }
}
