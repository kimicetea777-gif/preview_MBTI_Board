package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AS(Authorization Server) 로그인 성공 후 서버 세션 부트스트랩.
 * 주의: 계정 조회/생성은 CustomOAuth2UserService.loadUser()에서 이미 끝난 뒤이므로 여기선 항상 존재해야 함.
 * AS access/refresh token 자체는 RedisOAuth2AuthorizedClientService가 oauth2Login() 기본 저장 경로를 통해
 * 이미 Redis에 저장했으므로 여기서 따로 다룰 필요 없음(예전처럼 즉시 폐기하지 않고 계속 보관).
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REGISTRATION_ID = "thebluebutter";

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SessionCookieUtils sessionCookieUtils;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final AppRoleSyncService appRoleSyncService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${authserver.refresh-token-ttl-days:30}")
    private long sessionCookieMaxAgeDays;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication) throws IOException {

        final OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        final UUID authServerId = UUID.fromString(oidcUser.getSubject());
        final User user = userRepository.findByAuthServerId(authServerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 로그인 시점에 막 발급된 access token의 app_role을 반영해둔다 — 안 하면 다음 refresh(최대
        // access token TTL 이후)까지는 세션이 예전 로컬 role로 부트스트랩된 채 남는다.
        final OAuth2AuthorizedClient authorizedClient =
                authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, authServerId.toString());
        if (authorizedClient != null) {
            appRoleSyncService.syncFromAccessToken(user, authorizedClient.getAccessToken().getTokenValue());
        }

        final String deviceId = UUID.randomUUID().toString();

        // Spring의 oauth2Login() 자동 저장은 authServerId 단독 키로만 저장한다(디바이스 인지 불가) —
        // 여기서 deviceId가 막 생겼으니 같은 토큰을 합성 키로 복사 저장하고, 원본은 지운다(fail-loud:
        // 마이그레이션 빠진 코드가 stale한 원본을 조용히 계속 읽는 걸 방지).
        if (authorizedClient != null) {
            final String devicePrincipal = AuthorizedClientRefreshService.devicePrincipalName(authServerId, deviceId);
            authorizedClientService.saveAuthorizedClient(
                    authorizedClient, new UsernamePasswordAuthenticationToken(devicePrincipal, null));
            authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, authServerId.toString());
        }

        final String sessionId = sessionService.createSession(user, deviceId, oidcUser.getIdToken().getTokenValue());

        sessionCookieUtils.addSessionCookie(response, sessionId, Duration.ofDays(sessionCookieMaxAgeDays).toMillis());

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/callback");
    }
}
