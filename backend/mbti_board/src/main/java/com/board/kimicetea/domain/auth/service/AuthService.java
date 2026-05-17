package com.board.kimicetea.domain.auth.service;

import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.board.kimicetea.domain.mbti.repository.MbtiMetadataRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.MbtiVerified;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

/**
 * 로컬 인증 비즈니스 로직 (회원가입·로그인·재발급·로그아웃)
 * 주의: 로그인 시 socialProvider가 있는 계정은 로컬 로그인 차단 (소셜 전용 계정)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String NONE_MBTI_ID = "NONE";

    private final UserRepository userRepository;
    private final MbtiMetadataRepository mbtiMetadataRepository;
    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final PasswordEncoder passwordEncoder;
    private final CookieUtils cookieUtils;
    private final TokenIssuerService tokenIssuerService;

    @Transactional
    public void signup(final SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        final MbtiMetadata mbti = mbtiMetadataRepository.findById(request.mbtiId())
                .orElseThrow(() -> new CustomException(ErrorCode.MBTI_NOT_FOUND));

        final MbtiVerified mbtiVerified = NONE_MBTI_ID.equals(request.mbtiId())
                ? MbtiVerified.DEFAULT
                : MbtiVerified.SELF;

        final User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .mbtiVerified(mbtiVerified)
                .role(UserRole.USER)
                .state(UserState.NORMAL)
                .mbti(mbti)
                .build();

        userRepository.save(user);
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

        // Reuse Detection: Redis에 없는 RT로 재발급 요청 → 해당 userId 전체 RT 폐기
        if (stored == null) {
            tokenRedisService.deleteAllRefreshTokens(userId);
            throw new CustomException(ErrorCode.REUSE_DETECTED);
        }
        if (!stored.equals(oldRefreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
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
            default -> {
                /* NORMAL, PENDING, SHADOW_BANNED 는 통과 */ }
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
