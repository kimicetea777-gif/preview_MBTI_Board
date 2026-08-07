package com.board.kimicetea.global.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizedClientServiceTest {

    private static final String REGISTRATION_ID = "thebluebutter";
    private static final String PRINCIPAL_NAME = "9c1bdd2e-1111-4a11-8a11-000000000001";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private RedisOAuth2AuthorizedClientService service;

    @BeforeEach
    void setUp() {
        service = new RedisOAuth2AuthorizedClientService(
                redisTemplate, clientRegistrationRepository, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(service, "refreshTokenTtlDays", 30L);
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

    @Nested
    @DisplayName("saveAuthorizedClient — 저장")
    class Save {

        @Test
        @DisplayName("key는 AC:{registrationId}:{principalName} 형식이고 TTL은 설정된 refresh token 수명이다")
        void savesWithExpectedKeyAndTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            final OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                    clientRegistration(), PRINCIPAL_NAME,
                    accessToken("at-value"), refreshToken("rt-value"));

            service.saveAuthorizedClient(authorizedClient, new TestingAuthenticationToken(PRINCIPAL_NAME, null));

            final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).set(keyCaptor.capture(), anyString(), ttlCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo("AC:" + REGISTRATION_ID + ":" + PRINCIPAL_NAME);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(30));
        }
    }

    @Nested
    @DisplayName("loadAuthorizedClient — 조회")
    class Load {

        @Test
        @DisplayName("저장된 값을 그대로 역직렬화해 access/refresh token을 복원한다(라운드트립)")
        void roundTripsAccessAndRefreshToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(clientRegistration());

            final OAuth2AuthorizedClient original = new OAuth2AuthorizedClient(
                    clientRegistration(), PRINCIPAL_NAME,
                    accessToken("at-value"), refreshToken("rt-value"));

            final ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            service.saveAuthorizedClient(original, new TestingAuthenticationToken(PRINCIPAL_NAME, null));
            verify(valueOperations).set(anyString(), jsonCaptor.capture(), any(Duration.class));

            when(valueOperations.get("AC:" + REGISTRATION_ID + ":" + PRINCIPAL_NAME)).thenReturn(jsonCaptor.getValue());

            final OAuth2AuthorizedClient loaded = service.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getAccessToken().getTokenValue()).isEqualTo("at-value");
            assertThat(loaded.getRefreshToken()).isNotNull();
            assertThat(loaded.getRefreshToken().getTokenValue()).isEqualTo("rt-value");
        }

        @Test
        @DisplayName("Redis에 값이 없으면 null을 반환한다")
        void returnsNullWhenMissing() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            final OAuth2AuthorizedClient loaded = service.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);

            assertThat(loaded).isNull();
        }

        @Test
        @DisplayName("refresh token 없이 저장된 값도(client_credentials 등) 정상 복원한다")
        void handlesNullRefreshToken() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(clientRegistration());

            final OAuth2AuthorizedClient original = new OAuth2AuthorizedClient(
                    clientRegistration(), PRINCIPAL_NAME, accessToken("at-value"));

            final ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
            service.saveAuthorizedClient(original, new TestingAuthenticationToken(PRINCIPAL_NAME, null));
            verify(valueOperations).set(anyString(), jsonCaptor.capture(), any(Duration.class));
            when(valueOperations.get("AC:" + REGISTRATION_ID + ":" + PRINCIPAL_NAME)).thenReturn(jsonCaptor.getValue());

            final OAuth2AuthorizedClient loaded = service.loadAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getRefreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("removeAuthorizedClient — 삭제")
    class Remove {

        @Test
        @DisplayName("해당 key를 삭제한다")
        void deletesExpectedKey() {
            service.removeAuthorizedClient(REGISTRATION_ID, PRINCIPAL_NAME);

            verify(redisTemplate).delete("AC:" + REGISTRATION_ID + ":" + PRINCIPAL_NAME);
        }
    }

    private OAuth2AccessToken accessToken(final String value) {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, value, Instant.now(), Instant.now().plusSeconds(300),
                Set.of("openid", "profile"));
    }

    private OAuth2RefreshToken refreshToken(final String value) {
        return new OAuth2RefreshToken(value, Instant.now());
    }
}
