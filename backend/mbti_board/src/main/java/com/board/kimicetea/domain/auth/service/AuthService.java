package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import com.board.kimicetea.global.security.session.SessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * 서버 세션 로그아웃 로직 — 로그인/가입/비밀번호 재설정은 AS가 호스팅한다(connect_manual.md 0번 원칙).
 * 재발급(reissue) 개념 자체가 없어짐 — 브라우저는 opaque 세션 쿠키만 들고 있고, AS access token 갱신은
 * SessionAuthenticationFilter가 매 요청마다 조용히 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SessionService sessionService;
    private final SessionCookieUtils sessionCookieUtils;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final AuthorizedClientRefreshService authorizedClientRefreshService;

    /** RP-Initiated Logout(/connect/logout) 완료 후 AS가 돌려보낼 프론트 주소 — AS의 post-logout-redirect-uris에도 등록돼 있어야 함 */
    @Value("${frontend.url}")
    private String frontendUrl;

    /** 단일 기기 로그아웃 — 세션 삭제가 곧 즉시 무효화(블랙리스트 개념 자체가 필요 없음) */
    public LogoutResponse logout(final String sessionId, final HttpServletResponse response) {
        final Optional<SessionService.SessionData> data = sessionService.getSessionData(sessionId);
        data.ifPresent(d -> authorizedClientRefreshService.revokeForLogout(d.authServerId(), d.deviceId()));
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
        final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(issuerUri + "/connect/logout")
                .queryParam("post_logout_redirect_uri", frontendUrl);
        if (!isExpired(idToken)) {
            builder.queryParam("id_token_hint", idToken);
        }
        return builder.build().toUriString();
    }

    /**
     * id_token은 로그인 시점에 한 번만 발급받아 세션에 저장되고 이후 AS access token이 refresh돼도
     * 갱신되지 않는데(AuthorizedClientRefreshService의 refresh_token grant는 새 id_token을 안 돌려받음),
     * AS(Spring Authorization Server)의 ID Token 수명은 TokenSettings로 조정 불가능한 고정 30분이라
     * 세션이 그보다 오래 지속되면 반드시 만료된다. 만료된 id_token_hint를 그대로 보내면 AS가
     * /connect/logout에서 invalid_token으로 400(Whitelabel Error Page)을 던지므로, 만료됐으면
     * id_token_hint 파라미터 자체를 생략한다 — AS가 자체 세션 쿠키 기반 로그아웃 확인 절차로 대체 처리.
     */
    private boolean isExpired(final String idToken) {
        try {
            final String[] parts = idToken.split("\\.");
            final String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            final JsonNode exp = OBJECT_MAPPER.readTree(payload).get("exp");
            return exp == null || Instant.ofEpochSecond(exp.asLong()).isBefore(Instant.now());
        } catch (final Exception e) {
            log.warn("id_token exp 파싱 실패 — 만료로 간주하고 id_token_hint 생략", e);
            return true;
        }
    }
}
