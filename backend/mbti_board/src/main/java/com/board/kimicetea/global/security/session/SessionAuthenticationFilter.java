package com.board.kimicetea.global.security.session;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshService;
import com.board.kimicetea.global.security.oauth2.AuthorizedClientRefreshUnavailableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * 세션 쿠키로 매 요청을 인증한다 — {@code JwtAuthenticationFilter}를 대체.
 * 세션 쿠키 → SessionService 조회 → AS access token 만료 임박이면 조용히 refresh(AuthorizedClientRefreshService)
 * → refresh가 실제로 일어났을 때만 로컬 User를 재조회해 role/차단여부 캐시 갱신 → CustomUserDetails 등록.
 * 주의: Redis/AS 통신 장애는 세션을 지우지 않고 이번 요청만 미인증 처리한다(JwtAuthenticationFilter의
 * RedisConnectionFailureException 처리와 동일한 fail-closed-but-세션은-보존 원칙).
 */
@Slf4j
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    /** EntryPoint가 request.getAttribute(...)로 읽어 401 메시지를 구분하는 데 씀 */
    public static final String SESSION_ERROR_ATTRIBUTE = "session-error";

    private final SessionService sessionService;
    private final AuthorizedClientRefreshService authorizedClientRefreshService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {

        final Optional<String> sessionId = SessionCookieUtils.extractSessionId(request);
        if (sessionId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 세션 조회 자체도 Redis 호출이라 장애 시 여기서도 터질 수 있음 — authenticate()와 동일하게
        // "세션은 보존, 이번 요청만 미인증 처리" 원칙을 적용하기 위해 같은 try 블록으로 묶는다
        try {
            final Optional<SessionService.SessionData> sessionData = sessionService.getSessionData(sessionId.get());
            if (sessionData.isEmpty()) {
                request.setAttribute(SESSION_ERROR_ATTRIBUTE, "SESSION_INVALID");
            } else {
                authenticate(sessionId.get(), sessionData.get(), request);
            }
        } catch (final RedisConnectionFailureException | AuthorizedClientRefreshUnavailableException e) {
            log.error(AlertMarkers.REDIS_CONNECTION_FAIL, "세션 인증 중 일시 장애 — 세션은 유지하고 이번 요청만 실패 처리", e);
            request.setAttribute(SESSION_ERROR_ATTRIBUTE, "TEMPORARILY_UNAVAILABLE");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(final String sessionId, final SessionService.SessionData data,
                              final HttpServletRequest request) {
        final AuthorizedClientRefreshService.RefreshOutcome outcome =
                authorizedClientRefreshService.getFreshAuthorizedClient(data.authServerId());

        if (outcome.rejected()) {
            sessionService.deleteSession(sessionId);
            request.setAttribute(SESSION_ERROR_ATTRIBUTE, "SESSION_INVALID");
            return;
        }

        final SessionService.SessionData effective = outcome.refreshed()
                ? refreshRoleAndBlocked(sessionId, data)
                : data;

        if (effective.blocked()) {
            sessionService.deleteSession(sessionId);
            request.setAttribute(SESSION_ERROR_ATTRIBUTE, "ACCOUNT_BLOCKED");
            return;
        }

        registerAuthentication(effective);
    }

    /** AS 토큰이 실제로 refresh된 요청에서만 호출 — 로컬 User를 다시 읽어 role/차단여부 캐시를 갱신 */
    private SessionService.SessionData refreshRoleAndBlocked(final String sessionId, final SessionService.SessionData current) {
        final User user = userRepository.findById(current.userId()).orElse(null);
        if (user == null) {
            return current;
        }
        final String role = user.getRole().name();
        final boolean blocked = user.getState().isBlocked();
        sessionService.updateRoleAndBlocked(sessionId, current, role, blocked);
        return new SessionService.SessionData(
                current.userId(), current.authServerId(), current.email(), current.deviceId(), current.idToken(),
                role, blocked);
    }

    private void registerAuthentication(final SessionService.SessionData data) {
        final CustomUserDetails userDetails = new CustomUserDetails(
                data.userId(), data.email(), null, data.role(), data.deviceId());
        final UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
