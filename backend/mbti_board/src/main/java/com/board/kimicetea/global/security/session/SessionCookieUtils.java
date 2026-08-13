package com.board.kimicetea.global.security.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * HttpOnly 세션 쿠키 생성/삭제/파싱 유틸리티. 옛 RT 쿠키(CookieUtils)와 달리 매 API 요청마다
 * 필요하므로 Path=/ 전체에 걸리고, "자동 로그인" 개념 자체가 없어 rememberMe 분기 없이 항상 영속 쿠키다
 * (Max-Age = AS refresh token 수명, memo2.md 3-3절).
 * 옛 설계가 HttpOnly RT 쿠키 옆에 프론트가 읽을 수 있는 비-HttpOnly {@code session=1} 힌트 쿠키를
 * 같이 뒀던 것과 동일하게, 실제 sessionId(HttpOnly)와 별개로 로그인 여부만 알려주는 힌트 쿠키도 같이
 * 발급/삭제한다 — 프론트가 실제 세션 값을 못 읽어도 로그인 상태 UI 분기는 여전히 동기적으로 판단해야 함.
 */
@Component
public class SessionCookieUtils {

    public static final String SESSION_COOKIE_NAME = "sessionId";
    private static final String SESSION_HINT_COOKIE_NAME = "session";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";

    @Value("${app.cookie.secure:false}")
    private boolean secure;

    // 프론트(typebutter.thebluebutter.com)가 이 힌트 쿠키로 로그인 여부를 판단하는데(proxy.ts),
    // 백엔드는 다른 서브도메인(api-typebutter.thebluebutter.com)에서 응답하므로 Domain을 안 넓히면
    // host-only 쿠키가 되어 프론트 도메인 요청엔 실리지 않는다. 로컬(localhost)은 빈 값으로 두어
    // host-only를 유지 — localhost에 "."로 시작하는 Domain을 주면 오히려 브라우저가 쿠키를 거부한다.
    // sessionId(HttpOnly) 쿠키는 항상 api-typebutter.thebluebutter.com로만 요청되므로(apiClient의
    // baseURL) host-only 그대로 둬도 무방 — 굳이 넓혀서 노출 범위를 키우지 않는다.
    @Value("${app.cookie.domain:}")
    private String hintCookieDomain;

    public void addSessionCookie(final HttpServletResponse response, final String sessionId, final long maxAgeMillis) {
        final ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, sessionId)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        final ResponseCookie hintCookie = ResponseCookie.from(SESSION_HINT_COOKIE_NAME, "1")
                .httpOnly(false)
                .secure(secure)
                .domain(hintCookieDomain.isBlank() ? null : hintCookieDomain)
                .path(COOKIE_PATH)
                .sameSite(SAME_SITE)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, hintCookie.toString());
    }

    public void clearSessionCookie(final HttpServletResponse response) {
        final ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        final ResponseCookie hintCookie = ResponseCookie.from(SESSION_HINT_COOKIE_NAME, "")
                .httpOnly(false)
                .secure(secure)
                .domain(hintCookieDomain.isBlank() ? null : hintCookieDomain)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite(SAME_SITE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, hintCookie.toString());
    }

    public static Optional<String> extractSessionId(final HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> SESSION_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
