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

    /** RT Cookie 발급 — HttpOnly, SameSite=Lax, Secure(환경별) */
    public void addRefreshTokenCookie(final HttpServletResponse response,
                                      final String refreshToken,
                                      final long maxAgeMillis) {
        final ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
