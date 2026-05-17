package com.board.kimicetea.global.security.jwt;

import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.security.redis.TokenRedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 매 요청마다 AT를 검증하고 SecurityContext에 인증 정보를 등록
 * 주의: 블랙리스트 확인은 Redis 조회를 수반하므로 permitAll 경로는 SecurityConfig에서 제외 권장
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenRedisService tokenRedisService;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {

        final String token = resolveToken(request);

        if (token != null) {
            // validateAndParseClaims로 단일 파싱 — isValid() + parseToken() 이중 파싱 방지
            final boolean authenticated = jwtProvider.validateAndParseClaims(token)
                    .filter(claims -> !tokenRedisService.isBlacklisted(token))
                    .map(claims -> { registerAuthentication(claims); return true; })
                    .orElse(false);

            // AT가 있었지만 유효하지 않음 — EntryPoint에서 원인 구분용
            if (!authenticated) {
                request.setAttribute("jwt-error", "TOKEN_INVALID");
            }
        }

        filterChain.doFilter(request, response);
    }

    private void registerAuthentication(final Claims claims) {
        final CustomUserDetails userDetails = new CustomUserDetails(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                null,
                claims.get("role", String.class),
                claims.get("deviceId", String.class)
        );
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(final HttpServletRequest request) {
        final String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
