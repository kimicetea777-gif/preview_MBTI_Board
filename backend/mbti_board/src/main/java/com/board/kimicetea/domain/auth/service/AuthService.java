package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

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

    /** RP-Initiated Logout(/connect/logout) 완료 후 AS가 돌려보낼 프론트 주소 — AS의 post-logout-redirect-uris에도 등록돼 있어야 함 */
    @Value("${frontend.url}")
    private String frontendUrl;

    /** 단일 기기 로그아웃 — 세션 삭제가 곧 즉시 무효화(블랙리스트 개념 자체가 필요 없음) */
    public LogoutResponse logout(final String sessionId, final HttpServletResponse response) {
        final Optional<SessionService.SessionData> data = sessionService.getSessionData(sessionId);
        sessionService.deleteSession(sessionId);
        sessionCookieUtils.clearSessionCookie(response);

        return data.map(SessionService.SessionData::idToken)
                .map(this::buildAuthServerLogoutUrl)
                .map(LogoutResponse::new)
                .orElseGet(() -> new LogoutResponse(null));
    }

    /**
     * 연동된 모든 기기 로그아웃. 다른 기기의 AS 세션까지는 이 요청(현재 브라우저)만으로 끝낼 방법이 없으니,
     * 지금 이 기기의 AS 세션만 RP-Initiated Logout으로 정리하고 나머지 기기는 세션 삭제로 자연 재로그인을 유도한다.
     */
    public LogoutResponse logoutAll(final String sessionId, final HttpServletResponse response) {
        final Optional<SessionService.SessionData> data = sessionService.getSessionData(sessionId);
        data.ifPresent(d -> sessionService.deleteAllSessions(d.authServerId()));
        sessionCookieUtils.clearSessionCookie(response);

        return data.map(SessionService.SessionData::idToken)
                .map(this::buildAuthServerLogoutUrl)
                .map(LogoutResponse::new)
                .orElseGet(() -> new LogoutResponse(null));
    }

    private String buildAuthServerLogoutUrl(final String idToken) {
        final String issuerUri = clientRegistrationRepository.findByRegistrationId("thebluebutter")
                .getProviderDetails().getIssuerUri();
        return UriComponentsBuilder.fromUriString(issuerUri + "/connect/logout")
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", frontendUrl)
                .build().toUriString();
    }
}
