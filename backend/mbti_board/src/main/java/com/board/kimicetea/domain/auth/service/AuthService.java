package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

/**
 * 로컬 인증 비즈니스 로직 (회원가입·로그인·재발급·로그아웃)
 * 주의: 유저 생성·닉네임 중복 검증은 UserService에 위임 — AuthService는 토큰 발급/폐기만 담당
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtils cookieUtils;
    private final TokenIssuerService tokenIssuerService;

    /** 회원가입 — 유저 생성은 UserService에 위임 */
    @Transactional
    public void signup(final SignupRequest request) {
        userService.registerUser(request.email(), request.password(), request.nickname(), request.mbtiId());
    }

    @Transactional
    public LoginResponse login(final LoginRequest request, final HttpServletResponse response) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getSocialProvider() != null) {
            throw new CustomException(ErrorCode.SOCIAL_ONLY_ACCOUNT);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        validateAccountState(user);

        final String deviceId = UUID.randomUUID().toString();
        final String accessToken = tokenIssuerService.issue(user, deviceId, response);

        user.updateLastLoginAt();

        return new LoginResponse(accessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

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
            throw new CustomException(ErrorCode.REUSE_DETECTED);
        }

        final User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // RT Rotation: 기존 RT 삭제 후 새 AT/RT 발급
        tokenRedisService.deleteRefreshToken(userId, deviceId);

        final String newAccessToken = tokenIssuerService.issue(user, deviceId, response);

        return new LoginResponse(newAccessToken, user.getEmail(), user.getNickname(), user.getRole().name());
    }

    /** AT 블랙리스트 등록 + Redis RT 삭제 + 클라이언트 Cookie 만료 처리 */
    public void logout(final String accessToken, final String userId,
            final String deviceId, final HttpServletResponse response) {
        tokenRedisService.addToBlacklist(accessToken, jwtProvider.getRemainingExpiry(accessToken));
        tokenRedisService.deleteRefreshToken(userId, deviceId);
        cookieUtils.clearRefreshTokenCookie(response);
    }

    /**
     * 계정 상태 검증 — 로그인 불가 상태 차단
     * 주의: SHADOW_BANNED는 콘텐츠 노출 제한일 뿐, 로그인 자체는 허용
     */
    private void validateAccountState(final User user) {
        switch (user.getState()) {
            case LOCKED -> throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
            case PERMANENT_BAN -> throw new CustomException(ErrorCode.ACCOUNT_BANNED);
            case INACTIVE -> throw new CustomException(ErrorCode.ACCOUNT_INACTIVE);
            case WITHDRAWN -> throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
            default -> { /* NORMAL, PENDING, SHADOW_BANNED 는 통과 */ }
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
