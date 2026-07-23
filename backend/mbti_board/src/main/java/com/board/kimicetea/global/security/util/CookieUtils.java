package com.board.kimicetea.global.security.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * HttpOnly RT 쿠키 생성/삭제 유틸리티
 * 주의: app.cookie.secure=false(기본값)는 로컬 HTTP 전용. 프로덕션 HTTPS 환경에서는 반드시 true로 설정
 */
@Component
public class CookieUtils {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Lax";

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    /**
     * RT Cookie 발급 — HttpOnly, SameSite=Lax, Secure(환경별)
     * 주의: rememberMe=false면 Max-Age를 생략해 브라우저 세션 쿠키로 발급 (브라우저 종료 시 삭제).
     *       RT 자체의 Redis TTL은 rememberMe 여부와 무관하게 항상 maxAgeMillis로 고정됨
     */
    public void addRefreshTokenCookie(final HttpServletResponse response,
                                      final String refreshToken,
                                      final long maxAgeMillis,
                                      final boolean rememberMe) {
        final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE);
        if (rememberMe) {
            builder.maxAge(Duration.ofMillis(maxAgeMillis));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /** 로그아웃 시 클라이언트 RT Cookie 즉시 만료 */
    public void clearRefreshTokenCookie(final HttpServletResponse response) {
        final ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
