package com.board.kimicetea.global.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * AS(Authorization Server) access/refresh token을 Redis에 보관하는 {@link OAuth2AuthorizedClientService} 구현체.
 * Spring Boot 기본(in-memory) 구현은 재시작 시 로그인 세션이 통째로 날아가는 문제가 있어 대체한다.
 * Key: AC:{registrationId}:{principalName}(=AS sub). TTL: AS refresh token 수명(설정값)으로 저장할 때마다 슬라이딩.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private static final String AC_PREFIX = "AC:";

    private final StringRedisTemplate redisTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ObjectMapper objectMapper;

    @Value("${authserver.refresh-token-ttl-days:30}")
    private long refreshTokenTtlDays;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(final String registrationId, final String principalName) {
        final String json = redisTemplate.opsForValue().get(key(registrationId, principalName));
        if (json == null) {
            return null;
        }

        final ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (clientRegistration == null) {
            return null;
        }

        try {
            final AuthorizedClientData data = objectMapper.readValue(json, AuthorizedClientData.class);
            final OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    data.accessTokenValue(),
                    data.accessTokenIssuedAt(),
                    data.accessTokenExpiresAt(),
                    data.scopes());
            final OAuth2RefreshToken refreshToken = data.refreshTokenValue() == null
                    ? null
                    : new OAuth2RefreshToken(data.refreshTokenValue(), data.refreshTokenIssuedAt());
            return (T) new OAuth2AuthorizedClient(clientRegistration, principalName, accessToken, refreshToken);
        } catch (final JsonProcessingException e) {
            log.error("AS authorized client 역직렬화 실패 (registrationId={}, principalName={})", registrationId, principalName, e);
            return null;
        }
    }

    /**
     * 이 메서드가 매 refresh(≈5분 주기)마다 rotation된 새 refresh_token과 함께 실제로 호출되므로,
     * TTL을 매번 다시 세팅하는 것만으로 세션의 실질 idle timeout이 자연히 슬라이딩된다.
     */
    @Override
    public void saveAuthorizedClient(final OAuth2AuthorizedClient authorizedClient, final Authentication principal) {
        final OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        final OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        final AuthorizedClientData data = new AuthorizedClientData(
                accessToken.getTokenValue(),
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt(),
                accessToken.getScopes(),
                refreshToken != null ? refreshToken.getTokenValue() : null,
                refreshToken != null ? refreshToken.getIssuedAt() : null);

        try {
            final String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(
                    key(authorizedClient.getClientRegistration().getRegistrationId(), principal.getName()),
                    json,
                    Duration.ofDays(refreshTokenTtlDays));
        } catch (final JsonProcessingException e) {
            log.error("AS authorized client 직렬화 실패 (registrationId={})",
                    authorizedClient.getClientRegistration().getRegistrationId(), e);
        }
    }

    @Override
    public void removeAuthorizedClient(final String registrationId, final String principalName) {
        redisTemplate.delete(key(registrationId, principalName));
    }

    private String key(final String registrationId, final String principalName) {
        return AC_PREFIX + registrationId + ":" + principalName;
    }

    private record AuthorizedClientData(
            String accessTokenValue,
            Instant accessTokenIssuedAt,
            Instant accessTokenExpiresAt,
            Set<String> scopes,
            String refreshTokenValue,
            Instant refreshTokenIssuedAt) {
    }
}
