package com.board.kimicetea.global.membershipsync;

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
 * AS의 {@code GET /api/internal/membership-events}를 client_credentials 토큰으로 호출하는 클라이언트.
 * {@code typebutter-deletion-sync} client(scope에 membership-events:read 추가됨)를 DeletedUserPollClient와
 * 공유하므로 같은 {@code deletionSyncAuthorizedClientManager}를 재사용한다.
 */
@Component
public class MembershipEventPollClient {

    private static final String REGISTRATION_ID = "typebutter-deletion-sync";
    private static final String MEMBERSHIP_EVENTS_PATH = "/api/internal/membership-events";

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager deletionSyncAuthorizedClientManager;

    public MembershipEventPollClient(
            @Value("${authserver.issuer-uri}") final String issuerUri,
            @Qualifier("deletionSyncAuthorizedClientManager") final OAuth2AuthorizedClientManager deletionSyncAuthorizedClientManager) {
        this.restClient = RestClient.create(issuerUri);
        this.deletionSyncAuthorizedClientManager = deletionSyncAuthorizedClientManager;
    }

    /**
     * @throws org.springframework.web.client.RestClientException AS 통신 실패(네트워크/5xx 등) —
     *         호출부(MembershipEventSyncService)가 "이번 실행만 실패, 커서는 건드리지 않음"으로 처리해야 함
     * @throws IllegalStateException client_credentials 토큰 자체를 못 받은 경우
     */
    public MembershipEventsPollResponse poll(final Instant since, final Long sinceId, final int limit) {
        final String accessToken = fetchAccessToken();
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(MEMBERSHIP_EVENTS_PATH)
                        .queryParam("since", since)
                        .queryParamIfPresent("sinceId", Optional.ofNullable(sinceId))
                        .queryParam("limit", limit)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(MembershipEventsPollResponse.class);
    }

    private String fetchAccessToken() {
        final OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(REGISTRATION_ID)
                .build();
        final OAuth2AuthorizedClient authorizedClient = deletionSyncAuthorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("멤버십 이벤트 폴링용 client_credentials 토큰 발급 실패");
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
