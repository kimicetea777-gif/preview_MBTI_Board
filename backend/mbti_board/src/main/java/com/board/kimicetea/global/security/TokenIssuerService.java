package com.board.kimicetea.global.security;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.global.security.jwt.JwtProvider;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import com.board.kimicetea.global.security.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AT·RT 발급 공통 서비스 — 로컬 로그인·OAuth2 로그인에서 공유
 * 주의: 토큰 발급·Redis 저장·쿠키 설정을 하나의 단위로 처리
 */
@Service
@RequiredArgsConstructor
public class TokenIssuerService {

    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;
    private final CookieUtils cookieUtils;

    /** AT·RT 발급 → Redis 저장 → 쿠키 설정 후 AT 반환 */
    public String issue(final User user, final String deviceId, final HttpServletResponse response) {
        final String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), deviceId);
        final String refreshToken = jwtProvider.generateRefreshToken(user.getId(), deviceId);

        tokenRedisService.saveRefreshToken(
                user.getId().toString(), deviceId, refreshToken, jwtProvider.getRefreshExpiry());
        cookieUtils.addRefreshTokenCookie(response, refreshToken, jwtProvider.getRefreshExpiry());

        return accessToken;
    }
}
