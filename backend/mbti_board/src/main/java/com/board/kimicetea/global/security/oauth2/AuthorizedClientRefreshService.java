package com.board.kimicetea.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AS access token 만료 임박 시에만 {@code OAuth2AuthorizedClientManager}로 조용히 refresh한다.
 * 요청 스레드 밖(세션 조회로 재구성한 principalName만 있는 상황)에서 쓰기 위해
 * {@link AuthorizedClientServiceOAuth2AuthorizedClientManager}(request-aware하지 않음)를 refresh 전용
 * provider만으로 구성해서 쓴다.
 * 주의: 동시에 여러 요청이 같은 유저의 만료 임박 토큰을 보고 동시에 refresh를 시도하면 AS의 refresh token
 * rotation 특성상 하나만 성공하고 나머지는 재사용감지로 세션 전체가 죽을 수 있어 분산 락으로 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizedClientRefreshService {

    private static final String REGISTRATION_ID = "thebluebutter";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60); // RefreshTokenOAuth2AuthorizedClientProvider 기본값과 동일하게 맞춤

    private static final String LOCK_PREFIX = "refresh-lock:";
    private static final Duration LOCK_TTL = Duration.ofMillis(2500);
    private static final int MAX_POLL_ATTEMPTS = 3;
    private static final long POLL_INTERVAL_MS = 40;

    // 락 소유자(값)가 일치할 때만 삭제 — 다른 요청이 새로 잡은 락을 실수로 풀지 않기 위함
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

    // 요청 스레드 밖에서 principalName만으로 refresh 가능한 request-unaware 매니저 — SecurityConfig에서 @Bean으로 구성
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RedisOAuth2AuthorizedClientService authorizedClientService;
    private final StringRedisTemplate redisTemplate;

    /**
     * @throws RedisConnectionFailureException Redis 장애 — 세션은 건드리지 말고 이번 요청만 실패시켜야 함
     * @throws AuthorizedClientRefreshUnavailableException AS 서버 통신 자체가 실패(네트워크/타임아웃) — 위와 동일하게 처리
     */
    public RefreshOutcome getFreshAuthorizedClient(final UUID authServerId) {
        final String principalName = authServerId.toString();

        final OAuth2AuthorizedClient current = authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
        if (current == null) {
            return RefreshOutcome.ofRejected();
        }
        if (!isExpiringSoon(current)) {
            return RefreshOutcome.unchanged(current);
        }

        final String lockKey = LOCK_PREFIX + principalName;
        final String lockToken = UUID.randomUUID().toString();

        if (tryLock(lockKey, lockToken)) {
            try {
                return refreshNow(principalName, current);
            } finally {
                releaseLock(lockKey, lockToken);
            }
        }

        // 락을 못 잡음 — 다른 요청이 이미 refresh 중이므로 짧게 폴링하며 갱신된 값을 기다린다
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            sleep(POLL_INTERVAL_MS);
            final OAuth2AuthorizedClient polled = authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
            if (polled != null && !isExpiringSoon(polled)) {
                return RefreshOutcome.unchanged(polled);
            }
        }

        // 폴링으로도 못 얻었으면(락 소유자가 예상보다 오래 걸리는 등) 직접 시도
        return refreshNow(principalName, current);
    }

    private RefreshOutcome refreshNow(final String principalName, final OAuth2AuthorizedClient current) {
        try {
            final OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                    .principal(principalName)
                    .build();
            final OAuth2AuthorizedClient refreshed = authorizedClientManager.authorize(request);
            // null = provider가 "재인가 불필요"로 판단(이미 유효) — 이번 refresh 시도는 안 한 것으로 취급
            return refreshed == null ? RefreshOutcome.unchanged(current) : RefreshOutcome.ofRefreshed(refreshed);
        } catch (final OAuth2AuthorizationException e) {
            log.warn("AS refresh 거부 (principalName={}): {}", principalName, e.getError());
            return RefreshOutcome.ofRejected();
        } catch (final RedisConnectionFailureException e) {
            throw e;
        } catch (final RestClientException e) {
            throw new AuthorizedClientRefreshUnavailableException("AS 서버 통신 실패", e);
        }
    }

    private boolean isExpiringSoon(final OAuth2AuthorizedClient authorizedClient) {
        final Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
        return expiresAt == null || Instant.now().isAfter(expiresAt.minus(CLOCK_SKEW));
    }

    private boolean tryLock(final String key, final String token) {
        final Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseLock(final String key, final String token) {
        redisTemplate.execute(RELEASE_LOCK, List.of(key), token);
    }

    private void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record RefreshOutcome(OAuth2AuthorizedClient authorizedClient, boolean refreshed, boolean rejected) {
        static RefreshOutcome unchanged(final OAuth2AuthorizedClient client) {
            return new RefreshOutcome(client, false, false);
        }

        static RefreshOutcome ofRefreshed(final OAuth2AuthorizedClient client) {
            return new RefreshOutcome(client, true, false);
        }

        static RefreshOutcome ofRejected() {
            return new RefreshOutcome(null, false, true);
        }
    }
}
