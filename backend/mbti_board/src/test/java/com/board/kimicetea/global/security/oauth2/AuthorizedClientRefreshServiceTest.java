package com.board.kimicetea.global.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizedClientRefreshServiceTest {

    private static final String REGISTRATION_ID = "thebluebutter";
    private static final UUID AUTH_SERVER_ID = UUID.randomUUID();
    private static final String DEVICE_ID = "device-001";
    private static final String PRINCIPAL_NAME = AUTH_SERVER_ID + ":" + DEVICE_ID;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private RedisOAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private AuthServerTokenRevocationClient revocationClient;

    private AuthorizedClientRefreshService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizedClientRefreshService(authorizedClientManager, authorizedClientService, revocationClient);
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("TypeButter")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/thebluebutter")
                .authorizationUri("http://localhost:9000/oauth2/authorize")
                .tokenUri("http://localhost:9000/oauth2/token")
                .scope("openid", "profile")
                .build();
    }

    private OAuth2AuthorizedClient clientExpiringIn(final Duration remaining) {
        final OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "at-value", Instant.now(), Instant.now().plus(remaining),
                Set.of("openid", "profile"));
        return new OAuth2AuthorizedClient(clientRegistration(), PRINCIPAL_NAME, accessToken);
    }

    @Nested
    @DisplayName("만료 임박이 아닐 때")
    class NotExpiringSoon {

        @Test
        @DisplayName("refresh를 시도하지 않고 기존 값을 그대로 반환한다")
        void returnsUnchangedWithoutRefreshing() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofMinutes(4));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            assertThat(outcome.refreshed()).isFalse();
            assertThat(outcome.rejected()).isFalse();
            assertThat(outcome.authorizedClient()).isSameAs(current);
            verifyNoInteractions(authorizedClientManager);
        }
    }

    @Nested
    @DisplayName("저장된 authorized client가 없을 때")
    class NoAuthorizedClient {

        @Test
        @DisplayName("바로 rejected를 반환한다")
        void returnsRejected() {
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(null);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            assertThat(outcome.rejected()).isTrue();
            assertThat(outcome.authorizedClient()).isNull();
            verifyNoInteractions(authorizedClientManager);
        }
    }

    @Nested
    @DisplayName("만료 임박 — 단독 요청")
    class ExpiringSoonSingleRequest {

        @Test
        @DisplayName("실제로 refresh를 수행한다")
        void refreshes() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshed = clientExpiringIn(Duration.ofMinutes(5));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(refreshed);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            assertThat(outcome.refreshed()).isTrue();
            assertThat(outcome.authorizedClient()).isSameAs(refreshed);

            final ArgumentCaptor<OAuth2AuthorizeRequest> requestCaptor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
            verify(authorizedClientManager).authorize(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
            assertThat(requestCaptor.getValue().getPrincipal().getName()).isEqualTo(PRINCIPAL_NAME);
        }

        @Test
        @DisplayName("provider가 null(재인가 불필요)을 반환하면 기존 값을 유지한 것으로 취급한다")
        void managerReturningNullIsTreatedAsUnchanged() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            assertThat(outcome.refreshed()).isFalse();
            assertThat(outcome.rejected()).isFalse();
            assertThat(outcome.authorizedClient()).isSameAs(current);
        }

        @Test
        @DisplayName("AS가 명시적으로 거부(OAuth2AuthorizationException)하면 rejected를 반환한다")
        void asRejectionReturnsRejected() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new OAuth2AuthorizationException(new OAuth2Error("invalid_grant")));

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            assertThat(outcome.rejected()).isTrue();
        }

        @Test
        @DisplayName("Redis 장애는 세션을 건드리지 않도록 그대로 전파한다")
        void redisFailurePropagates() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new RedisConnectionFailureException("boom"));

            assertThatThrownBy(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID))
                    .isInstanceOf(RedisConnectionFailureException.class);
        }

        @Test
        @DisplayName("AS 통신 실패(RestClientException)는 AuthorizedClientRefreshUnavailableException으로 감싸 전파한다")
        void networkFailureWrapsIntoUnavailableException() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new ResourceAccessException("connect timed out"));

            assertThatThrownBy(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID))
                    .isInstanceOf(AuthorizedClientRefreshUnavailableException.class)
                    .hasCauseInstanceOf(ResourceAccessException.class);
        }
    }

    @Nested
    @DisplayName("만료 임박 — 같은 principalName에 대한 동시 요청(coalescing)")
    class ConcurrentRequestsAreCoalesced {

        @Test
        @DisplayName("동시에 여러 요청이 오면 AS에는 한 번만 나가고, 모두 같은 결과를 받는다")
        void coalescesConcurrentRequestsIntoOneAsCall() throws Exception {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshed = clientExpiringIn(Duration.ofMinutes(5));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);

            final CountDownLatch enteredAuthorize = new CountDownLatch(1);
            final CountDownLatch releaseAuthorize = new CountDownLatch(1);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenAnswer(invocation -> {
                enteredAuthorize.countDown();
                assertThat(releaseAuthorize.await(2, TimeUnit.SECONDS)).isTrue();
                return refreshed;
            });

            final ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                final Future<AuthorizedClientRefreshService.RefreshOutcome> first =
                        executor.submit(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID));
                // 대표 스레드가 실제로 AS 호출(authorize())에 진입할 때까지 기다린 뒤에야 두 번째 요청을 보낸다 —
                // 그래야 두 번째 요청이 확실히 "이미 진행 중인 refresh"를 만나 합쳐지는 경로를 탄다.
                assertThat(enteredAuthorize.await(2, TimeUnit.SECONDS)).isTrue();

                final CountDownLatch secondThreadStarting = new CountDownLatch(1);
                final Future<AuthorizedClientRefreshService.RefreshOutcome> second = executor.submit(() -> {
                    secondThreadStarting.countDown();
                    return service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);
                });
                assertThat(secondThreadStarting.await(2, TimeUnit.SECONDS)).isTrue();
                // secondThreadStarting은 "스레드가 시작됐다"만 보장하고 "putIfAbsent까지 마쳤다"는 보장하지
                // 않는다 — 그 사이 간극은 OS 스레드 스케줄링 지연에 비하면 무시할 수준이라 짧은 여유만 둔다.
                // 대표 스레드(첫 번째)는 이 시점까지 계속 releaseAuthorize에 막혀 있으므로, 둘 다 맵에 등록을
                // 마칠 때까지는 대표 스레드가 끝나서 자기 엔트리를 지우는 일이 생기지 않는다.
                Thread.sleep(100);

                releaseAuthorize.countDown();

                final AuthorizedClientRefreshService.RefreshOutcome outcome1 = first.get(2, TimeUnit.SECONDS);
                final AuthorizedClientRefreshService.RefreshOutcome outcome2 = second.get(2, TimeUnit.SECONDS);

                assertThat(outcome1.authorizedClient()).isSameAs(refreshed);
                assertThat(outcome2.authorizedClient()).isSameAs(refreshed);
                verify(authorizedClientManager, times(1)).authorize(any(OAuth2AuthorizeRequest.class));
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("대표 요청의 refresh가 예외로 실패하면(Redis 장애 등), 기다리던 요청도 같은 예외를 받는다")
        void waitingRequestReceivesSameExceptionAsRepresentative() throws Exception {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);

            final CountDownLatch enteredAuthorize = new CountDownLatch(1);
            final CountDownLatch releaseAuthorize = new CountDownLatch(1);
            // RedisConnectionFailureException은 refreshNow()가 잡았다가 그대로 다시 던지는 예외라(rejected로
            // 변환되지 않음) — coalescing된 Future가 예외로 완료되고 대기자도 그 예외를 받는지 검증하기 적합하다.
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenAnswer(invocation -> {
                enteredAuthorize.countDown();
                assertThat(releaseAuthorize.await(2, TimeUnit.SECONDS)).isTrue();
                throw new RedisConnectionFailureException("boom");
            });

            final ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                final Future<AuthorizedClientRefreshService.RefreshOutcome> first =
                        executor.submit(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID));
                assertThat(enteredAuthorize.await(2, TimeUnit.SECONDS)).isTrue();

                final CountDownLatch secondThreadStarting = new CountDownLatch(1);
                final Future<AuthorizedClientRefreshService.RefreshOutcome> second = executor.submit(() -> {
                    secondThreadStarting.countDown();
                    return service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);
                });
                assertThat(secondThreadStarting.await(2, TimeUnit.SECONDS)).isTrue();
                Thread.sleep(100);

                releaseAuthorize.countDown();

                assertThatThrownBy(() -> first.get(2, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(RedisConnectionFailureException.class);
                assertThatThrownBy(() -> second.get(2, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(RedisConnectionFailureException.class);
                verify(authorizedClientManager, times(1)).authorize(any(OAuth2AuthorizeRequest.class));
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("먼저 온 요청의 refresh가 끝나 맵에서 정리된 뒤에는, 다음 요청이 새로 refresh를 시도한다")
        void doesNotReuseStaleFutureAfterCompletion() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshed = clientExpiringIn(Duration.ofMinutes(5));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(refreshed);

            service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);
            service.getFreshAuthorizedClient(AUTH_SERVER_ID, DEVICE_ID);

            verify(authorizedClientManager, times(2)).authorize(any(OAuth2AuthorizeRequest.class));
        }
    }

    @Test
    @DisplayName("ReflectionTestUtils로 필드가 생성자로만 채워짐을 확인")
    void constructorWiresFieldsOnly() {
        assertThat(ReflectionTestUtils.getField(service, "authorizedClientManager")).isSameAs(authorizedClientManager);
        assertThat(ReflectionTestUtils.getField(service, "authorizedClientService")).isSameAs(authorizedClientService);
        assertThat(ReflectionTestUtils.getField(service, "revocationClient")).isSameAs(revocationClient);
    }

    @Nested
    @DisplayName("revokeForLogout")
    class RevokeForLogout {

        private OAuth2AuthorizedClient clientWithRefreshToken() {
            return new OAuth2AuthorizedClient(
                    clientRegistration(), PRINCIPAL_NAME,
                    new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "at-value", Instant.now(),
                            Instant.now().plus(Duration.ofMinutes(5))),
                    new OAuth2RefreshToken("rt-value", Instant.now()));
        }

        @Test
        @DisplayName("저장된 refresh_token을 AS에 revoke 요청하고 로컬 AC 엔트리도 제거한다")
        void revokesAndRemovesLocalEntry() {
            final OAuth2AuthorizedClient current = clientWithRefreshToken();
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);

            service.revokeForLogout(AUTH_SERVER_ID, DEVICE_ID);

            verify(revocationClient).revokeRefreshToken("TypeButter", "secret", "rt-value");
            verify(authorizedClientService).removeAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);
        }

        @Test
        @DisplayName("저장된 authorized client가 없으면 아무것도 하지 않는다")
        void noOpWhenNoAuthorizedClient() {
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(null);

            service.revokeForLogout(AUTH_SERVER_ID, DEVICE_ID);

            verifyNoInteractions(revocationClient);
            verify(authorizedClientService, never()).removeAuthorizedClient(anyString(), anyString());
        }

        @Test
        @DisplayName("refresh_token이 없으면 아무것도 하지 않는다")
        void noOpWhenNoRefreshToken() {
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME))
                    .thenReturn(clientExpiringIn(Duration.ofMinutes(5)));

            service.revokeForLogout(AUTH_SERVER_ID, DEVICE_ID);

            verifyNoInteractions(revocationClient);
            verify(authorizedClientService, never()).removeAuthorizedClient(anyString(), anyString());
        }

        @Test
        @DisplayName("revoke가 RestClientException을 던져도 로컬 AC는 정리된다")
        void removesLocalEntryEvenWhenRevokeFails() {
            final OAuth2AuthorizedClient current = clientWithRefreshToken();
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            doThrow(new ResourceAccessException("connect timed out"))
                    .when(revocationClient).revokeRefreshToken(anyString(), anyString(), anyString());

            service.revokeForLogout(AUTH_SERVER_ID, DEVICE_ID);

            verify(authorizedClientService).removeAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);
        }

        @Test
        @DisplayName("Redis 장애가 나도 예외가 밖으로 새지 않는다(fail-open)")
        void redisFailureDoesNotPropagate() {
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME))
                    .thenThrow(new RedisConnectionFailureException("boom"));

            service.revokeForLogout(AUTH_SERVER_ID, DEVICE_ID);

            verifyNoInteractions(revocationClient);
        }
    }
}
