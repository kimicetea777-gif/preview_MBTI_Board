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

    /**
     * [역할] AS 내부 API 호출용 client_credentials 액세스 토큰 값을 돌려준다.
     * 이미 발급된 토큰이 유효하면 재사용하고, 만료됐을 때만 새로 발급받는다
     * ({@code deletionSyncAuthorizedClientManager}가 처리).
     *
     * <p>[주의사항]
     * <ul>
     *   <li>client_credentials에는 로그인한 사용자가 없으므로 principal 이름을 등록 ID로 고정한다.
     *       (OAuth2AuthorizeRequest는 principal이 반드시 있어야 한다)</li>
     *   <li>매니저는 request-unaware 타입이다. 스케줄러 스레드에서 호출되므로 HttpServletRequest에 의존하는
     *       매니저로 바꾸면 요청 컨텍스트가 없어 동작하지 않는다.</li>
     *   <li>매니저가 null을 돌려주면 토큰을 못 받은 것이므로 {@link IllegalStateException}을 던진다.
     *       호출부는 이를 "이번 폴링 실행 실패"로 처리해야 한다.</li>
     * </ul>
     *
     * @throws IllegalStateException client_credentials 토큰 발급에 실패한 경우
     */
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
