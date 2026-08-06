package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;
import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.security.TokenIssuerService;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.UUID;

/**
 * MBTI_Board 자체 세션(AT/RT) 비즈니스 로직 (재발급·로그아웃) — AS(Authorization Server)와는 무관하게
 * 이 서버 자신의 토큰만 다룬다. 로그인/가입/비밀번호 재설정은 AS가 호스팅한다(connect_manual.md 0번 원칙).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final CookieUtils cookieUtils;
    private final TokenIssuerService tokenIssuerService;
    private final AuditLogService auditLogService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    /** RP-Initiated Logout(/connect/logout) 완료 후 AS가 돌려보낼 프론트 주소 — AS의 post-logout-redirect-uris에도 등록돼 있어야 함 */
    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    public LoginResponse reissue(final HttpServletRequest request, final HttpServletResponse response) {
        final String oldRefreshToken = extractRefreshTokenFromCookie(request);

        final Claims claims = parseRefreshTokenOrThrow(oldRefreshToken);
        final String userId = claims.getSubject();
        final String deviceId = claims.get("deviceId", String.class);

        final String stored = tokenRedisService.getRefreshToken(userId, deviceId);

        if (stored == null) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        if (!stored.equals(oldRefreshToken)) {
            tokenRedisService.deleteAllRefreshTokens(userId);
            auditLogService.record(AuditEvent.REUSE_DETECTED, UUID.fromString(userId),
                    "UNKNOWN", RequestUtils.extractClientIp(request));
            throw new CustomException(ErrorCode.REUSE_DETECTED);
        }

        final User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateAccountAccessible(user);

        // RT Rotation: 기존 RT 삭제 후 새 AT/RT 발급
        tokenRedisService.deleteRefreshToken(userId, deviceId);

        // 기존 RT의 rememberMe 값을 새 RT/쿠키에도 그대로 유지 (클레임 없는 구버전 토큰은 유지 로그인으로 간주)
        final Boolean rememberMeClaim = claims.get("rememberMe", Boolean.class);
        final boolean rememberMe = rememberMeClaim == null || rememberMeClaim;
        final String newAccessToken = tokenIssuerService.issue(user, deviceId, response, rememberMe);

        return new LoginResponse(newAccessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    /**
     * AT 블랙리스트 등록 + Redis RT 삭제 + 클라이언트 Cookie 만료 처리.
     * 소셜 로그인으로 저장해둔 AS ID Token이 있으면 AS RP-Initiated Logout URL도 같이 만들어 반환한다 —
     * 그래야 프론트가 그 주소로 이동시켜 AS 브라우저 세션까지 끝내고, 다음 로그인 때 (자동 로그인 없이)
     * 로그인 화면을 다시 보여줄 수 있다.
     */
    public LogoutResponse logout(final String accessToken, final String userId,
            final String deviceId, final HttpServletResponse response) {
        // 만료된 AT는 블랙리스트 등록 불필요 — 필터에서 이미 거부됨
        jwtProvider.validateAndParseClaims(accessToken).ifPresent(claims -> {
            final long remaining = jwtProvider.getRemainingExpiry(claims);
            if (remaining > 0) {
                tokenRedisService.addToBlacklist(claims.getId(), remaining);
            }
        });
        tokenRedisService.deleteRefreshToken(userId, deviceId);
        cookieUtils.clearRefreshTokenCookie(response);

        final String idToken = tokenRedisService.getIdToken(userId, deviceId);
        if (idToken == null) {
            return new LogoutResponse(null);
        }
        tokenRedisService.deleteIdToken(userId, deviceId);
        return new LogoutResponse(buildAuthServerLogoutUrl(idToken));
    }

    private String buildAuthServerLogoutUrl(final String idToken) {
        final String issuerUri = clientRegistrationRepository.findByRegistrationId("thebluebutter")
                .getProviderDetails().getIssuerUri();
        return UriComponentsBuilder.fromUriString(issuerUri + "/connect/logout")
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", frontendUrl)
                .build().toUriString();
    }

    /**
     * 연동된 모든 기기 로그아웃 — 현재 AT 블랙리스트 등록 + 전 기기 RT 삭제 + 현재 기기 Cookie 만료 처리.
     * 다른 기기의 AS 세션까지는 이 요청(현재 브라우저)만으로 끝낼 방법이 없으니, 지금 이 기기의 AS 세션만
     * RP-Initiated Logout으로 정리하고 나머지 기기의 ID Token은 그냥 삭제만 해서(자연 재로그인 유도) 정합성을 맞춘다.
     * 주의: 공용 PC 등에서 로그아웃을 깜빡한 다른 세션까지 사용자가 직접 정리할 수 있도록 마이페이지에서 제공
     */
    public LogoutResponse logoutAll(final String accessToken, final String userId,
            final String deviceId, final HttpServletResponse response) {
        jwtProvider.validateAndParseClaims(accessToken).ifPresent(claims -> {
            final long remaining = jwtProvider.getRemainingExpiry(claims);
            if (remaining > 0) {
                tokenRedisService.addToBlacklist(claims.getId(), remaining);
            }
        });

        final String idToken = tokenRedisService.getIdToken(userId, deviceId);
        tokenRedisService.deleteAllRefreshTokens(userId);
        tokenRedisService.deleteAllIdTokens(userId);
        cookieUtils.clearRefreshTokenCookie(response);

        return idToken == null ? new LogoutResponse(null) : new LogoutResponse(buildAuthServerLogoutUrl(idToken));
    }

    /**
     * 계정 접근 가능 여부 검증 — INACTIVE·LOCKED·PERMANENT_BAN·WITHDRAWN 상태는 사용자에게 사유를 알리고 차단
     * 주의: SHADOW_BANNED는 콘텐츠 노출 제한일 뿐, 접근 자체는 허용
     */
    private void validateAccountAccessible(final User user) {
        switch (user.getState()) {
            case LOCKED -> throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
            case PERMANENT_BAN -> throw new CustomException(ErrorCode.ACCOUNT_BANNED);
            case INACTIVE -> throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
            case WITHDRAWN -> throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
            default -> {
            }
        }
    }

    private String extractRefreshTokenFromCookie(final HttpServletRequest request) {
        if (request.getCookies() == null)
            throw new CustomException(ErrorCode.MISSING_REFRESH_TOKEN);
        return Arrays.stream(request.getCookies())
                .filter(c -> CookieUtils.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_REFRESH_TOKEN));
    }

    private Claims parseRefreshTokenOrThrow(final String token) {
        return jwtProvider.validateAndParseClaims(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
    }
}
