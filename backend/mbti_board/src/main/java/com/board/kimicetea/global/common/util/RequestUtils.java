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

    /**
     * Cloudflare Tunnel 환경: CF-Connecting-IP만 신뢰, 없으면 RemoteAddr
     * 주의: CF-Connecting-IP는 Cloudflare가 실제 클라이언트 IP로 항상 덮어써서 전달하는 헤더 —
     *       DB/Redis/백엔드 포트가 127.0.0.1로만 열려있어 Cloudflare Tunnel을 거치지 않은 요청은
     *       애초에 도달할 수 없다는 전제 하에 신뢰함. 포트를 다시 외부에 열면 이 전제가 깨짐.
     */
    public static String extractClientIp(final HttpServletRequest request) {
        final String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.strip();
        }
        // X-Forwarded-For는 클라이언트가 임의로 조작해서 보낼 수 있어 기본적으로 신뢰하지 않음.
        // nginx·다른 CDN 등 X-Forwarded-For를 신뢰할 수 있는 리버스 프록시로 전환 시에만 아래를 활성화할 것
        // final String forwarded = request.getHeader("X-Forwarded-For");
        // if (forwarded != null && !forwarded.isBlank()) {
        //     return forwarded.split(",")[0].strip();
        // }
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
