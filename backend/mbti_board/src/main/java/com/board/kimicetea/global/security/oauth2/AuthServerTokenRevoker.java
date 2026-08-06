package com.board.kimicetea.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * AS(Authorization Server) 로그인 직후 받은 access/refresh token은 {@link OAuth2AuthenticationSuccessHandler}가
 * MBTI_Board 자체 AT/RT를 새로 발급하는 순간부터 다시는 쓰이지 않는다 — 그대로 두면 AS의
 * oauth2_authorization row가 만료 시각까지 불필요하게 살아있으므로, 발급 직후 여기서 바로 폐기한다.
 * 주의: AS 쪽 정리가 실패해도 이미 발급한 MBTI_Board 자체 토큰에는 영향이 없어야 하므로 로그인 흐름과
 * 분리된 비동기 best-effort로 처리한다(AsyncConfig의 AsyncUncaughtExceptionHandler가 실패를 로깅).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServerTokenRevoker {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient = RestClient.create();

    @Async
    public void revoke(final String registrationId, final String principalName) {
        final OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(registrationId, principalName);
        if (authorizedClient == null) {
            return;
        }

        final OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
        if (refreshToken == null) {
            return;
        }

        final ClientRegistration clientRegistration = authorizedClient.getClientRegistration();
        final String revocationUri = clientRegistration.getProviderDetails().getIssuerUri() + "/oauth2/revoke";

        final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", refreshToken.getTokenValue());
        body.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(revocationUri)
                    .headers(headers -> headers.setBasicAuth(
                            clientRegistration.getClientId(), clientRegistration.getClientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (final Exception e) {
            log.warn("AS refresh token revoke 실패 (registrationId={})", registrationId, e);
            return;
        }

        authorizedClientService.removeAuthorizedClient(registrationId, principalName);
    }
}
