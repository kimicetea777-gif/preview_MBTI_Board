package com.board.kimicetea.global.security.config;

import com.board.kimicetea.global.security.oauth2.RedisOAuth2AuthorizedClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * request-unaware {@code OAuth2AuthorizedClientManager} 빈들 전용 설정.
 * <p>
 * 원래 {@code SecurityConfig}에 있었으나, {@code SecurityConfig}가 생성자로 주입받는
 * {@code AuthorizedClientRefreshService}가 바로 이 빈들을 의존하면서 순환 참조가 생겼다
 * ({@code SecurityConfig} → {@code AuthorizedClientRefreshService} → (이 클래스에 있던)
 * {@code @Bean} 메서드 호출 → 그 메서드를 갖고 있던 {@code SecurityConfig} 인스턴스 필요 → 순환).
 * 이 빈들을 {@code SecurityConfig}와 무관한 별도 설정 클래스로 분리해서 순환 고리를 끊는다.
 */
@Configuration
public class OAuth2ClientManagerConfig {

    /**
     * 요청 스레드 밖(세션 조회로 재구성한 principalName만 있는 상황)에서 AS access token을 refresh
     * 전용으로 갱신하기 위한 매니저 — request-aware한 DefaultOAuth2AuthorizedClientManager가 아님.
     * AuthorizedClientRefreshService가 이 빈을 통해서만 refresh를 시도한다.
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            final ClientRegistrationRepository clientRegistrationRepository,
            final RedisOAuth2AuthorizedClientService authorizedClientService) {
        final AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build());
        return manager;
    }

    /**
     * AS 서버-to-서버 폴링(typebutter-deletion-sync, client_credentials 전용) 토큰 발급 매니저.
     * 로그인 토큰용 authorizedClientManager와 완전히 별개 — DeletedUserPollClient·MembershipEventPollClient가
     * 이 빈을 공유한다(같은 client, scope만 deletion:read + membership-events:read로 확장).
     */
    @Bean
    OAuth2AuthorizedClientManager deletionSyncAuthorizedClientManager(
            final ClientRegistrationRepository clientRegistrationRepository,
            final RedisOAuth2AuthorizedClientService authorizedClientService) {
        final AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    /**
     * AS access token(JWT)의 app_role claim을 로컬 role에 동기화하려고 서명을 검증하며 디코딩하는 용도
     * — {@code AppRoleSyncService} 전용. 발급자(issuer) 기준으로 AS의 jwks_uri를 자동 탐색한다.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${authserver.issuer-uri}") final String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
