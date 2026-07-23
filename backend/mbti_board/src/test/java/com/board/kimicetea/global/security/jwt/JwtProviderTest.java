package com.board.kimicetea.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    // 256비트(32바이트) 이상 — HMAC-SHA256 최소 요구사항
    private static final String SECRET = "test-secret-key-that-is-at-least-32bytes!!";
    private static final long ACCESS_EXPIRY  = 1000L * 60 * 60;       // 1시간
    private static final long REFRESH_EXPIRY = 1000L * 60 * 60 * 24;  // 1일
    private static final long SIGNUP_EXPIRY  = 1000L * 60 * 10;       // 10분

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY, SIGNUP_EXPIRY);
    }

    @Nested
    @DisplayName("generateAccessToken — AT 생성 및 파싱")
    class GenerateAccessToken {

        @Test
        @DisplayName("생성된 AT를 파싱하면 전달한 claims와 일치한다")
        void claims_match() {
            final UUID userId   = UUID.randomUUID();
            final String email  = "test@test.com";
            final String role   = "USER";
            final String device = "device-01";

            final String token  = jwtProvider.generateAccessToken(userId, email, role, device);
            final Claims claims = jwtProvider.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo(email);
            assertThat(claims.get("role",  String.class)).isEqualTo(role);
            assertThat(claims.get("deviceId", String.class)).isEqualTo(device);
        }

        @Test
        @DisplayName("생성된 AT의 jti는 null이 아니다")
        void jti_isNotNull() {
            final String token  = jwtProvider.generateAccessToken(UUID.randomUUID(), "e@e.com", "USER", "d1");
            final Claims claims = jwtProvider.parseToken(token);

            assertThat(claims.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateOAuth2SignupToken — signupToken 생성 및 파싱")
    class GenerateOAuth2SignupToken {

        @Test
        @DisplayName("생성된 signupToken을 파싱하면 전달한 프로필 claims와 일치한다")
        void claims_match() {
            final String token = jwtProvider.generateOAuth2SignupToken(
                    "google", "provider-id-1", "test@test.com");
            final Claims claims = jwtProvider.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo("google:provider-id-1");
            assertThat(claims.get("purpose", String.class)).isEqualTo("OAUTH2_SIGNUP");
            assertThat(claims.get("provider", String.class)).isEqualTo("google");
            assertThat(claims.get("providerId", String.class)).isEqualTo("provider-id-1");
            assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
        }
    }

    @Nested
    @DisplayName("generateRefreshToken — RT 생성 및 파싱")
    class GenerateRefreshToken {

        @Test
        @DisplayName("생성된 RT를 파싱하면 userId와 deviceId가 일치한다")
        void claims_match() {
            final UUID userId   = UUID.randomUUID();
            final String device = "device-01";

            final String token  = jwtProvider.generateRefreshToken(userId, device, true);
            final Claims claims = jwtProvider.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("deviceId", String.class)).isEqualTo(device);
        }
    }

    @Nested
    @DisplayName("validateAndParseClaims — 검증 후 claims 반환")
    class ValidateAndParseClaims {

        @Test
        @DisplayName("유효한 토큰이면 Optional.of(claims)를 반환한다")
        void valid_returnsClaims() {
            final String token = jwtProvider.generateAccessToken(UUID.randomUUID(), "e@e.com", "USER", "d1");

            final Optional<Claims> result = jwtProvider.validateAndParseClaims(token);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("만료된 토큰이면 Optional.empty()를 반환한다")
        void expired_returnsEmpty() {
            final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            final String expiredToken = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(key)
                    .compact();

            assertThat(jwtProvider.validateAndParseClaims(expiredToken)).isEmpty();
        }

        @Test
        @DisplayName("서명이 위변조된 토큰이면 Optional.empty()를 반환한다")
        void tampered_returnsEmpty() {
            final String token = jwtProvider.generateAccessToken(UUID.randomUUID(), "e@e.com", "USER", "d1");
            final String[] parts = token.split("\\.");
            final String tampered = parts[0] + "." + parts[1] + ".invalidsignature";

            assertThat(jwtProvider.validateAndParseClaims(tampered)).isEmpty();
        }

        @Test
        @DisplayName("형식이 잘못된 문자열이면 Optional.empty()를 반환한다")
        void malformed_returnsEmpty() {
            assertThat(jwtProvider.validateAndParseClaims("not.a.jwt")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRemainingExpiry — 남은 만료 시간 계산")
    class GetRemainingExpiry {

        @Test
        @DisplayName("유효한 AT이면 남은 시간이 0보다 크다")
        void valid_remainingIsPositive() {
            final String token  = jwtProvider.generateAccessToken(UUID.randomUUID(), "e@e.com", "USER", "d1");
            final Claims claims = jwtProvider.parseToken(token);

            assertThat(jwtProvider.getRemainingExpiry(claims)).isPositive();
        }

        @Test
        @DisplayName("만료된 claims이면 0을 반환한다")
        void expired_returnsZero() {
            final Claims expiredClaims = mock(Claims.class);
            given(expiredClaims.getExpiration()).willReturn(new Date(System.currentTimeMillis() - 5000));

            assertThat(jwtProvider.getRemainingExpiry(expiredClaims)).isZero();
        }
    }
}
