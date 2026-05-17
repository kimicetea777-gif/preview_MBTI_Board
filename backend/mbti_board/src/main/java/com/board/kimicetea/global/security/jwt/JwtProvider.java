package com.board.kimicetea.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT Access Token / Refresh Token 생성·검증·파싱 담당
 * 주의: JWT_SECRET은 반드시 환경변수로만 주입
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessExpiry;
    private final long refreshExpiry;

    public JwtProvider(
            @Value("${jwt.secret}") final String secret,
            @Value("${jwt.access-expiry}") final long accessExpiry,
            @Value("${jwt.refresh-expiry}") final long refreshExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
    }

    public String generateAccessToken(final UUID userId, final String email,
            final String role, final String deviceId) {
        final Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("deviceId", deviceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(final UUID userId, final String deviceId) {
        final Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("deviceId", deviceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(final String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 서명·만료 검증 후 Claims 반환 — 필터에서 단일 파싱으로 사용
     * 유효하지 않으면 Optional.empty() 반환 (예외를 던지지 않음)
     */
    public Optional<Claims> validateAndParseClaims(final String token) {
        try {
            return Optional.of(parseToken(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // public boolean isValid(final String token) {
    // return validateAndParseClaims(token).isPresent();
    // }

    /** AT 만료까지 남은 시간(ms) — 블랙리스트 TTL 계산용 */
    public long getRemainingExpiry(final String token) {
        final Date expiration = parseToken(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public long getRefreshExpiry() {
        return refreshExpiry;
    }
}
