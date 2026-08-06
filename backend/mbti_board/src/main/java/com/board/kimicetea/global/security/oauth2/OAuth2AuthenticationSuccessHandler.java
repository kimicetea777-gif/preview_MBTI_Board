package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.TokenIssuerService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

/**
 * AS(Authorization Server) 로그인 성공 후 MBTI_Board 자체 AT/RT 발급.
 * 주의: 계정 조회/생성은 CustomOAuth2UserService.loadUser()에서 이미 끝난 뒤이므로 여기선 항상 존재해야 함
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenIssuerService tokenIssuerService;
    private final UserRepository userRepository;
    private final AuthServerTokenRevoker authServerTokenRevoker;
    private final TokenRedisService tokenRedisService;
    private final JwtProvider jwtProvider;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication) throws IOException {

        final OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        final UUID authServerId = UUID.fromString(oidcUser.getSubject());
        final User user = userRepository.findByAuthServerId(authServerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        final String deviceId = UUID.randomUUID().toString();
        // 소셜 로그인은 별도 체크박스 UI가 없으므로 기본 유지 로그인으로 발급
        final String accessToken = tokenIssuerService.issue(user, deviceId, response, true);

        // AS ID Token은 access/refresh token과 달리 로그아웃 시 RP-Initiated Logout(/connect/logout)의
        // id_token_hint로 필요하므로 별도 보관한다 — MBTI_Board 자체 RT와 생명주기를 맞춰 같은 TTL로 저장
        tokenRedisService.saveIdToken(
                user.getId().toString(), deviceId, oidcUser.getIdToken().getTokenValue(), jwtProvider.getRefreshExpiry());

        // MBTI_Board 자체 토큰을 발급했으니 AS access/refresh token은 더 이상 쓸 일이 없다 — 방치하지 않고 바로 폐기
        // (revoke는 refresh_token만 폐기하고 id_token은 건드리지 않으므로, 위에서 따로 저장해둔 id_token은 영향 없음)
        final OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;
        authServerTokenRevoker.revoke(
                oauth2Authentication.getAuthorizedClientRegistrationId(), oauth2Authentication.getName());

        final String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .fragment("accessToken=" + accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
