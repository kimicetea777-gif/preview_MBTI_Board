package com.board.kimicetea.global.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HTTP 요청 관련 유틸
 * 주의: Cloudflare Tunnel 환경 전제 — X-Forwarded-For를 신뢰하여 IP 추출
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils {

    /** Cloudflare Tunnel 환경: X-Forwarded-For 우선, 없으면 RemoteAddr */
    public static String extractClientIp(final HttpServletRequest request) {
        final String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    /** salt + ip를 SHA-256 해싱하여 Hex 문자열 반환 */
    public static String hashIp(final String salt, final String ip) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest((salt + ip).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
