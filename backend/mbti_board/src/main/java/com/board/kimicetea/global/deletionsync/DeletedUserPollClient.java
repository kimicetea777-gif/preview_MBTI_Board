package com.board.kimicetea.global.deletionsync;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;

/**
 * AS의 {@code GET /api/internal/deleted-users}(connect_manual.md §8-3)를 client_credentials 토큰으로
 * 호출하는 클라이언트. 토큰 발급/재사용은 {@code deletionSyncAuthorizedClientManager}(request-unaware,
 * SecurityConfig에 정의)에 위임 — {@code AuthorizedClientRefreshService.refreshNow()}와 동일한
 * {@code OAuth2AuthorizeRequest} 구성 패턴.
 */
@Component
public class DeletedUserPollClient {

    private static final String REGISTRATION_ID = "typebutter-deletion-sync";
    private static final String DELETED_USERS_PATH = "/api/internal/deleted-users";

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager deletionSyncAuthorizedClientManager;

    public DeletedUserPollClient(
            @Value("${authserver.issuer-uri}") final String issuerUri,
            @Qualifier("deletionSyncAuthorizedClientManager") final OAuth2AuthorizedClientManager deletionSyncAuthorizedClientManager) {
        this.restClient = RestClient.create(issuerUri);
        this.deletionSyncAuthorizedClientManager = deletionSyncAuthorizedClientManager;
    }

    /**
     * @throws org.springframework.web.client.RestClientException AS 통신 실패(네트워크/5xx 등) —
     *         호출부(DeletedUserSyncService)가 "이번 실행만 실패, 커서는 건드리지 않음"으로 처리해야 함
     * @throws IllegalStateException client_credentials 토큰 자체를 못 받은 경우
     */
    public DeletedUsersPollResponse poll(final Instant since, final Long sinceId, final int limit) {
        final String accessToken = fetchAccessToken();
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(DELETED_USERS_PATH)
                        .queryParam("since", since)
                        .queryParamIfPresent("sinceId", Optional.ofNullable(sinceId))
                        .queryParam("limit", limit)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(DeletedUsersPollResponse.class);
    }

    private String fetchAccessToken() {
        final OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(REGISTRATION_ID)
                .build();
        final OAuth2AuthorizedClient authorizedClient = deletionSyncAuthorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("AS 계정 하드 삭제 폴링용 client_credentials 토큰 발급 실패");
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
