package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 서버 세션 로그아웃 로직 — 로그인/가입/비밀번호 재설정은 AS가 호스팅한다(connect_manual.md 0번 원칙).
 * 재발급(reissue) 개념 자체가 없어짐 — 브라우저는 opaque 세션 쿠키만 들고 있고, AS access token 갱신은
 * SessionAuthenticationFilter가 매 요청마다 조용히 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SessionService sessionService;
    private final SessionCookieUtils sessionCookieUtils;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final AuthorizedClientRefreshService authorizedClientRefreshService;

    /** RP-Initiated Logout(/session/logout) 완료 후 AS가 돌려보낼 프론트 주소 — AS의 post-logout-redirect-uris에도 등록돼 있어야 함 */
    @Value("${frontend.url}")
    private String frontendUrl;

    /** 단일 기기 로그아웃 — 세션 삭제가 곧 즉시 무효화(블랙리스트 개념 자체가 필요 없음) */
    public LogoutResponse logout(final String sessionId, final HttpServletResponse response) {
        final Optional<SessionService.SessionData> data = sessionService.getSessionData(sessionId);
        data.ifPresent(d -> authorizedClientRefreshService.revokeForLogout(d.authServerId(), d.deviceId()));
        sessionService.deleteSession(sessionId);
        sessionCookieUtils.clearSessionCookie(response);

        return buildLogoutResponse(data.isPresent());
    }

    /**
     * 연동된 모든 기기 로그아웃. 다른 기기의 AS 세션까지는 이 요청(현재 브라우저)만으로 끝낼 방법이 없으니,
     * 지금 이 기기의 AS 세션만 RP-Initiated Logout으로 정리하고 나머지 기기는 세션 삭제로 자연 재로그인을 유도한다.
     */
    public LogoutResponse logoutAll(final String sessionId, final HttpServletResponse response) {
        final Optional<SessionService.SessionData> data = sessionService.getSessionData(sessionId);
        data.ifPresent(d -> sessionService.deleteAllSessions(d.authServerId()));
        sessionCookieUtils.clearSessionCookie(response);

        return buildLogoutResponse(data.isPresent());
    }

    private LogoutResponse buildLogoutResponse(final boolean hadSession) {
        return hadSession
                ? new LogoutResponse(buildAuthServerLogoutUrl())
                : new LogoutResponse(null);
    }

    /**
     * AS의 RP-Initiated Logout 주소. 표준 /connect/logout은 id_token_hint를 하드 요구하는데, 이 백엔드는
     * AS access token을 조용히 refresh할 때마다 AS가 저장된 id_token을 새 값으로 교체(rotate)해버려서
     * (OAuth2RefreshTokenAuthenticationProvider) 로그인 시점에 받아 세션에 넣어둔 id_token이 몇 분 만에
     * orphan이 된다 — Spring Security의 OAuth2AuthorizedClient에는 id_token 자리가 없어 refresh 응답의
     * 새 값을 붙들 수도 없다. 그래서 AS에 client_id + 세션 쿠키만으로 동작하는 전용 엔드포인트
     * (/session/logout)를 두고 이쪽을 쓴다 — AS 세션이 이미 만료된 뒤여도(로그인 후 30분 idle) 400 없이
     * post_logout_redirect_uri로 302한다.
     */
    private String buildAuthServerLogoutUrl() {
        final ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("thebluebutter");
        final String issuerUri = registration.getProviderDetails().getIssuerUri();
        return UriComponentsBuilder.fromUriString(issuerUri + "/session/logout")
                .queryParam("client_id", registration.getClientId())
                .queryParam("post_logout_redirect_uri", frontendUrl)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }
}
