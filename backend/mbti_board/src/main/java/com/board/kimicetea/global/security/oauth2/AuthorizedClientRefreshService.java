package com.board.kimicetea.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AS access token 만료 임박 시에만 {@code OAuth2AuthorizedClientManager}로 조용히 refresh한다.
 * 요청 스레드 밖(세션 조회로 재구성한 principalName만 있는 상황)에서 쓰기 위해
 * {@link AuthorizedClientServiceOAuth2AuthorizedClientManager}(request-aware하지 않음)를 refresh 전용
 * provider만으로 구성해서 쓴다.
 * 주의: 동시에 여러 요청이 같은 (기기의) 만료 임박 토큰을 보고 각자 refresh를 시도하면 AS의 refresh
 * token rotation 특성상 하나만 성공하고 나머지는 재사용감지로 세션 전체가 죽을 수 있다 — 그래서 같은
 * principalName에 대한 동시 요청은 {@link #inFlightRefreshes}로 합쳐서 실제 AS 호출이 한 번만 나가게
 * 막는다. 이건 같은 JVM 인스턴스 안에서만 유효하다 — 지금은 backend가 단일 인스턴스라 이걸로 충분하지만,
 * 레플리카가 여러 개로 늘어나면 인스턴스 간 경합은 이 방식으로 못 막으므로 그때는 Redis 기반 분산 락을
 * 별도로 다시 추가해야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizedClientRefreshService {

    private static final String REGISTRATION_ID = "thebluebutter";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60); // RefreshTokenOAuth2AuthorizedClientProvider 기본값과 동일하게 맞춤

    // 요청 스레드 밖에서 principalName만으로 refresh 가능한 request-unaware 매니저 — SecurityConfig에서 @Bean으로 구성
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final RedisOAuth2AuthorizedClientService authorizedClientService;
    private final AuthServerTokenRevocationClient revocationClient;

    // principalName별로 진행 중인 refresh를 하나로 합친다 — 같은 기기에서 거의 동시에 여러 요청이
    // 만료 임박을 감지해도 AS에는 실제로 한 번만 요청이 나가도록.
    private final ConcurrentHashMap<String, CompletableFuture<RefreshOutcome>> inFlightRefreshes = new ConcurrentHashMap<>();

    /** 디바이스별로 분리된 AC 엔트리를 가리키는 principal name — {@code authServerId:deviceId} 합성 키 */
    public static String devicePrincipalName(final UUID authServerId, final String deviceId) {
        return authServerId + ":" + deviceId;
    }

    /**
     * @throws RedisConnectionFailureException Redis 장애 — 세션은 건드리지 말고 이번 요청만 실패시켜야 함
     * @throws AuthorizedClientRefreshUnavailableException AS 서버 통신 자체가 실패(네트워크/타임아웃) — 위와 동일하게 처리
     */
    public RefreshOutcome getFreshAuthorizedClient(final UUID authServerId, final String deviceId) {
        final String principalName = devicePrincipalName(authServerId, deviceId);

        final OAuth2AuthorizedClient current = authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
        if (current == null) {
            return RefreshOutcome.ofRejected();
        }
        if (!isExpiringSoon(current)) {
            return RefreshOutcome.unchanged(current);
        }

        return coalescedRefresh(principalName, current);
    }

    /**
     * 같은 principalName에 대한 동시 요청을 합친다. 맵에 먼저 등록에 성공한 스레드(대표)만 실제로
     * {@link #refreshNow}를 수행하고, 뒤늦게 온 스레드들은 그 결과를 담을 {@link CompletableFuture}를
     * 그대로 기다렸다가 같은 결과를 돌려받는다.
     */
    private RefreshOutcome coalescedRefresh(final String principalName, final OAuth2AuthorizedClient current) {
        final CompletableFuture<RefreshOutcome> myFuture = new CompletableFuture<>();
        final CompletableFuture<RefreshOutcome> existing = inFlightRefreshes.putIfAbsent(principalName, myFuture);

        if (existing != null) {
            return awaitExisting(existing);
        }

        try {
            final RefreshOutcome outcome = refreshNow(principalName, current);
            myFuture.complete(outcome);
            return outcome;
        } catch (final RuntimeException e) {
            myFuture.completeExceptionally(e);
            throw e;
        } finally {
            inFlightRefreshes.remove(principalName, myFuture);
        }
    }

    /** future.join()이 원래 예외를 CompletionException으로 감싸므로, 대표 스레드가 던진 것과 같은 타입으로 풀어서 다시 던진다. */
    private RefreshOutcome awaitExisting(final CompletableFuture<RefreshOutcome> future) {
        try {
            return future.join();
        } catch (final CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    /**
     * 로그아웃 시 호출 — 현재 저장된 refresh_token을 AS에 revoke 요청하고 로컬 캐시도 정리한다.
     * id_token과 달리 이 값은 항상 최신이므로 별도로 새로 받아올 필요가 없다.
     * 전체를 try로 감싸는 이유: Redis 장애(loadAuthorizedClient/removeAuthorizedClient)든 AS
     * 통신 장애(revokeRefreshToken)든, 이 메서드의 실패가 호출부(AuthService.logout())까지
     * 전파돼 로그아웃 자체가 실패해버리면 안 됨 — 항상 fail-open.
     */
    public void revokeForLogout(final UUID authServerId, final String deviceId) {
        final String principalName = devicePrincipalName(authServerId, deviceId);
        try {
            final OAuth2AuthorizedClient current = authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
            if (current == null || current.getRefreshToken() == null) {
                return;
            }
            try {
                revocationClient.revokeRefreshToken(
                        current.getClientRegistration().getClientId(),
                        current.getClientRegistration().getClientSecret(),
                        current.getRefreshToken().getTokenValue());
            } catch (final RestClientException e) {
                log.warn("로그아웃 시 AS 토큰 revoke 실패 (principalName={}) — 로컬 캐시는 정리 진행", principalName, e);
            }
            // revoke 성공 여부와 무관하게 로컬 캐시는 정리한다(의도적) — 이 디바이스는 어차피
            // 재로그인해야 하므로, AS 쪽 revoke가 실패했다고 로컬에 살려둘 이유가 없다.
            authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, principalName);
        } catch (final RedisConnectionFailureException e) {
            log.warn("로그아웃 시 Redis 장애로 revoke 스킵 (principalName={})", principalName, e);
        }
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
