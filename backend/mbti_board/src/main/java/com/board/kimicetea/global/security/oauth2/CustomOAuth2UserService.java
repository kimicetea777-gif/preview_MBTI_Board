package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;
import com.board.kimicetea.global.common.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * AS(Authorization Server, client_id=TypeButter) 로그인 시 기존 회원 여부를 판별하는 서비스.
 * 주의: AS는 로그인/가입 시점에 이미 약관 동의를 끝내고서야 authorization code를 내려주므로
 *       (connect_manual.md 8-1), 여기서 신규 계정이면 별도 대기 상태 없이 즉시(JIT) 생성한다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final OAuth2AccountService oAuth2AccountService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public OidcUser loadUser(final OidcUserRequest userRequest) {
        final OidcUser oidcUser = super.loadUser(userRequest);
        final UUID authServerId = UUID.fromString(oidcUser.getSubject());

        final User user = userRepository.findByAuthServerId(authServerId)
                .map(existing -> {
                    validateAccountState(existing);
                    existing.updateLastLoginAt();
                    return existing;
                })
                .orElseGet(() -> oAuth2AccountService.createAccount(authServerId, oidcUser.getEmail()));

        auditLogService.record(AuditEvent.OAUTH2_LOGIN, user.getId(), user.getRole().name(), resolveClientIp());
        return oidcUser;
    }

    /** OAuth2 필터 체인은 요청 컨텍스트 내에서 실행되므로 RequestContextHolder로 IP 추출 가능 */
    private String resolveClientIp() {
        final ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "0.0.0.0";
        return RequestUtils.extractClientIp(attrs.getRequest());
    }

    /**
     * OAuth2 로그인 허용 불가 상태 차단 — AuthService.validateAccountAccessible()와 동일 정책
     * 주의: OAuth2AuthenticationException을 던져야 FailureHandler에서 에러 코드가 전달됨
     */
    private void validateAccountState(final User user) {
        switch (user.getState()) {
            case LOCKED        -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_LOCKED"));
            case PERMANENT_BAN -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_BANNED"));
            case INACTIVE      -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_INACTIVE"));
            case WITHDRAWN     -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_WITHDRAWN"));
            case MEMBERSHIP_WITHDRAWAL_PENDING ->
                    throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_WITHDRAWAL_PENDING"));
            default            -> { /* NORMAL, PENDING, SHADOW_BANNED 통과 */ }
        }
    }
}
