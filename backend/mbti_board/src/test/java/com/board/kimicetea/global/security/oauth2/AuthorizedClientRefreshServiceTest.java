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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizedClientRefreshServiceTest {

    private static final String REGISTRATION_ID = "thebluebutter";
    private static final UUID AUTH_SERVER_ID = UUID.randomUUID();
    private static final String PRINCIPAL_NAME = AUTH_SERVER_ID.toString();

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private RedisOAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthorizedClientRefreshService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizedClientRefreshService(authorizedClientManager, authorizedClientService, redisTemplate);
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

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.refreshed()).isFalse();
            assertThat(outcome.rejected()).isFalse();
            assertThat(outcome.authorizedClient()).isSameAs(current);
            verifyNoInteractions(authorizedClientManager, redisTemplate);
        }
    }

    @Nested
    @DisplayName("저장된 authorized client가 없을 때")
    class NoAuthorizedClient {

        @Test
        @DisplayName("바로 rejected를 반환한다")
        void returnsRejected() {
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(null);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.rejected()).isTrue();
            assertThat(outcome.authorizedClient()).isNull();
            verifyNoInteractions(authorizedClientManager, redisTemplate);
        }
    }

    @Nested
    @DisplayName("만료 임박 + 락 획득 성공")
    class ExpiringSoonWithLock {

        @Test
        @DisplayName("실제로 refresh를 수행하고, 끝나면 락을 해제한다")
        void refreshesAndReleasesLock() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshed = clientExpiringIn(Duration.ofMinutes(5));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(eq("refresh-lock:" + PRINCIPAL_NAME), anyString(), eq(Duration.ofMillis(2500))))
                    .thenReturn(true);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(refreshed);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.refreshed()).isTrue();
            assertThat(outcome.authorizedClient()).isSameAs(refreshed);

            final ArgumentCaptor<OAuth2AuthorizeRequest> requestCaptor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
            verify(authorizedClientManager).authorize(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getClientRegistrationId()).isEqualTo(REGISTRATION_ID);
            assertThat(requestCaptor.getValue().getPrincipal().getName()).isEqualTo(PRINCIPAL_NAME);

            verify(redisTemplate).execute(any(RedisScript.class), eq(List.of("refresh-lock:" + PRINCIPAL_NAME)), anyString());
        }

        @Test
        @DisplayName("provider가 null(재인가 불필요)을 반환하면 기존 값을 유지한 것으로 취급한다")
        void managerReturningNullIsTreatedAsUnchanged() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.refreshed()).isFalse();
            assertThat(outcome.rejected()).isFalse();
            assertThat(outcome.authorizedClient()).isSameAs(current);
        }

        @Test
        @DisplayName("AS가 명시적으로 거부(OAuth2AuthorizationException)하면 rejected를 반환한다")
        void asRejectionReturnsRejected() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new OAuth2AuthorizationException(new OAuth2Error("invalid_grant")));

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.rejected()).isTrue();
            // 실패해도 락은 반드시 해제되어야 함
            verify(redisTemplate).execute(any(RedisScript.class), anyList(), anyString());
        }

        @Test
        @DisplayName("Redis 장애는 세션을 건드리지 않도록 그대로 전파한다")
        void redisFailurePropagates() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new RedisConnectionFailureException("boom"));

            assertThatThrownBy(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .isInstanceOf(RedisConnectionFailureException.class);
        }

        @Test
        @DisplayName("AS 통신 실패(RestClientException)는 AuthorizedClientRefreshUnavailableException으로 감싸 전파한다")
        void networkFailureWrapsIntoUnavailableException() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                    .thenThrow(new ResourceAccessException("connect timed out"));

            assertThatThrownBy(() -> service.getFreshAuthorizedClient(AUTH_SERVER_ID))
                    .isInstanceOf(AuthorizedClientRefreshUnavailableException.class)
                    .hasCauseInstanceOf(ResourceAccessException.class);
        }
    }

    @Nested
    @DisplayName("만료 임박 + 락 획득 실패(다른 요청이 이미 refresh 중)")
    class ExpiringSoonWithoutLock {

        @Test
        @DisplayName("폴링하다가 갱신된 값을 발견하면 그 값을 반환하고 refresh를 직접 시도하지 않는다")
        void pollsAndFindsRefreshedValue() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshedByOther = clientExpiringIn(Duration.ofMinutes(5));
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME))
                    .thenReturn(current) // 최초 조회
                    .thenReturn(refreshedByOther); // 폴링 중 다른 요청이 갱신 완료
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.refreshed()).isFalse();
            assertThat(outcome.authorizedClient()).isSameAs(refreshedByOther);
            verifyNoInteractions(authorizedClientManager);
        }

        @Test
        @DisplayName("폴링해도 갱신된 값을 못 찾으면 결국 직접 refresh를 시도한다")
        void fallsBackToDirectRefreshAfterPolling() {
            final OAuth2AuthorizedClient current = clientExpiringIn(Duration.ofSeconds(10));
            final OAuth2AuthorizedClient refreshed = clientExpiringIn(Duration.ofMinutes(5));
            // 최초 조회 1회 + 폴링 3회 모두 여전히 만료 임박
            when(authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME)).thenReturn(current);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(refreshed);

            final AuthorizedClientRefreshService.RefreshOutcome outcome = service.getFreshAuthorizedClient(AUTH_SERVER_ID);

            assertThat(outcome.refreshed()).isTrue();
            assertThat(outcome.authorizedClient()).isSameAs(refreshed);
            verify(authorizedClientManager).authorize(any(OAuth2AuthorizeRequest.class));
            // 직접 시도한 경로는 락을 잡지 못했으므로 해제 스크립트도 실행하지 않아야 함
            verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString());
        }
    }

    @Test
    @DisplayName("ReflectionTestUtils로 필드가 생성자로만 채워짐을 확인(락 키 상수 재검증용 가드)")
    void constructorWiresFieldsOnly() {
        assertThat(ReflectionTestUtils.getField(service, "authorizedClientManager")).isSameAs(authorizedClientManager);
        assertThat(ReflectionTestUtils.getField(service, "authorizedClientService")).isSameAs(authorizedClientService);
        assertThat(ReflectionTestUtils.getField(service, "redisTemplate")).isSameAs(redisTemplate);
    }
}
