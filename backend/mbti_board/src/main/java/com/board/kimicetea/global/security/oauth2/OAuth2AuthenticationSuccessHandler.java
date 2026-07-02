package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.global.security.TokenIssuerService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

/**
 * OAuth2 로그인 성공 후 분기 처리
 * 주의: 기존 회원은 AT/RT를 발급하지만, 신규 가입자는 아직 계정이 없으므로
 *       AT 대신 짧은 만료의 signupToken만 발급해 약관 동의 페이지로 보냄
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenIssuerService tokenIssuerService;
    private final JwtProvider jwtProvider;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication) throws IOException {

        if (authentication.getPrincipal() instanceof PendingOAuth2SignupPrincipal pending) {
            final var userInfo = pending.getUserInfo();
            final String signupToken = jwtProvider.generateOAuth2SignupToken(
                    pending.getProvider(), userInfo.getProviderId(),
                    userInfo.getEmail(), userInfo.getNickname(), userInfo.getProfileImageUrl());

            final String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/terms/agree")
                    .fragment("signupToken=" + signupToken)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        final OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        final User user = principal.getUser();
        final String deviceId = UUID.randomUUID().toString();

        final String accessToken = tokenIssuerService.issue(user, deviceId, response);

        final String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .fragment("accessToken=" + accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
