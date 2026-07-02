package com.board.kimicetea.global.security.jwt;

import com.board.kimicetea.global.alert.AlertMarkers;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
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
    private final long signupExpiry;

    public JwtProvider(
            @Value("${jwt.secret}") final String secret,
            @Value("${jwt.access-expiry}") final long accessExpiry,
            @Value("${jwt.refresh-expiry}") final long refreshExpiry,
            @Value("${jwt.signup-expiry}") final long signupExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
        this.signupExpiry = signupExpiry;
    }

    public String generateAccessToken(final UUID userId, final String email,
            final String role, final String deviceId) {
        final Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
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
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("deviceId", deviceId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * OAuth2 신규 가입 약관 동의 대기용 임시 토큰 — DB에 아직 없는 프로필 정보를 담아 프론트로 전달
     * 주의: AT/RT와 달리 사용자 계정이 존재하지 않는 상태에서 발급되므로 subject는 provider:providerId 조합 사용
     */
    public String generateOAuth2SignupToken(final String provider, final String providerId,
            final String email, final String nickname, final String profileImageUrl) {
        final Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(provider + ":" + providerId)
                .claim("purpose", "OAUTH2_SIGNUP")
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("email", email)
                .claim("nickname", nickname)
                .claim("profileImageUrl", profileImageUrl)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + signupExpiry))
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
        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT");
            return Optional.empty();
        } catch (SignatureException | MalformedJwtException e) {
            log.error(AlertMarkers.JWT_TAMPER, "Tampered or malformed JWT: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** AT 만료까지 남은 시간(ms) — 블랙리스트 TTL 계산용. 이미 파싱된 Claims를 재사용해 이중 파싱 방지 */
    public long getRemainingExpiry(final Claims claims) {
        return Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
    }

    public long getRefreshExpiry() {
        return refreshExpiry;
    }
}
