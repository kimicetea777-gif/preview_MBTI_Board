package com.board.kimicetea.global.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * AS(Authorization Server)의 표준 OAuth2 Token Revocation(RFC 7009) 엔드포인트({@code /oauth2/revoke})를 호출하는 얇은 클라이언트.
 */
@Component
public class AuthServerTokenRevocationClient {

    private final RestClient restClient;

    public AuthServerTokenRevocationClient(@Value("${authserver.issuer-uri}") final String issuerUri) {
        this.restClient = RestClient.builder().baseUrl(issuerUri).build();
    }

    /** 로그아웃 시 refresh_token을 폐기 — AS가 매핑된 access_token까지 같이 무효화한다 */
    public void revokeRefreshToken(final String clientId, final String clientSecret, final String refreshToken) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        restClient.post()
                .uri("/oauth2/revoke")
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
