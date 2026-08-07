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
import org.springframework.security.core.Authentication;
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

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final SessionCookieUtils sessionCookieUtils;

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

        final String deviceId = UUID.randomUUID().toString();
        final String sessionId = sessionService.createSession(user, deviceId, oidcUser.getIdToken().getTokenValue());

        sessionCookieUtils.addSessionCookie(response, sessionId, Duration.ofDays(sessionCookieMaxAgeDays).toMillis());

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/callback");
    }
}
