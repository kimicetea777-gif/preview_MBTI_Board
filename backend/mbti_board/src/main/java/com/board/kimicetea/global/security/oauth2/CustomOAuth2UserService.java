package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.audit.AuditEvent;
import com.board.kimicetea.global.audit.AuditLogService;

import com.board.kimicetea.global.common.util.RequestUtils;
import com.board.kimicetea.global.security.oauth2.userinfo.GoogleOAuth2UserInfo;
import com.board.kimicetea.global.security.oauth2.userinfo.KakaoOAuth2UserInfo;
import com.board.kimicetea.global.security.oauth2.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 로그인 시 기존 회원 여부를 판별하는 서비스
 * 주의: social_provider + provider_id 조합으로 기존 회원 여부 판단.
 *       신규 회원은 여기서 User를 생성하지 않음 — 약관 동의 전 개인정보 DB 저장을 피하기 위해
 *       프로필 정보만 담은 PendingOAuth2SignupPrincipal을 반환하고, 실제 생성은
 *       약관 동의 완료 API(POST /api/auth/oauth2/signup, OAuth2AccountService)에서 수행됨
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public OAuth2User loadUser(final OAuth2UserRequest userRequest) {
        final OAuth2User oAuth2User = super.loadUser(userRequest);
        final String provider = userRequest.getClientRegistration().getRegistrationId();
        final OAuth2UserInfo userInfo = resolveUserInfo(provider, oAuth2User.getAttributes());

        final Optional<User> existing = userRepository
                .findBySocialProviderAndProviderId(provider, userInfo.getProviderId());

        if (existing.isPresent()) {
            final User user = existing.get();
            validateAccountState(user);
            user.updateLastLoginAt();
            auditLogService.record(AuditEvent.OAUTH2_LOGIN, user.getId(), user.getRole().name(), resolveClientIp());
            return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
        }

        // 신규 회원 — 가입 가능 여부만 검증(이메일 필수/중복). 실제 User 생성은 동의 완료 시점으로 미룸
        validateNewSignup(userInfo);
        return new PendingOAuth2SignupPrincipal(provider, userInfo);
    }

    /** OAuth2 필터 체인은 요청 컨텍스트 내에서 실행되므로 RequestContextHolder로 IP 추출 가능 */
    private String resolveClientIp() {
        final ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "0.0.0.0";
        return RequestUtils.extractClientIp(attrs.getRequest());
    }

    /**
     * OAuth2 로그인 허용 불가 상태 차단 — AuthService.validateAccountState()와 동일 정책
     * 주의: OAuth2AuthenticationException을 던져야 FailureHandler에서 에러 코드가 전달됨
     */
    private void validateAccountState(final User user) {
        switch (user.getState()) {
            case LOCKED        -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_LOCKED"));
            case PERMANENT_BAN -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_BANNED"));
            case INACTIVE      -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_INACTIVE"));
            case WITHDRAWN     -> throw new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_WITHDRAWN"));
            default            -> { /* NORMAL, PENDING, SHADOW_BANNED 통과 */ }
        }
    }

    private OAuth2UserInfo resolveUserInfo(final String provider, final Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("UNSUPPORTED_PROVIDER"));
        };
    }

    /**
     * 신규 가입 가능 여부만 검증 — 실제 User 생성은 하지 않음(동의 완료 시점으로 미룸)
     * 주의: OAuth2AuthenticationException을 던져야 FailureHandler에서 에러 코드가 전달됨
     */
    private void validateNewSignup(final OAuth2UserInfo userInfo) {
        // 이메일 미동의 또는 제공 불가 시 가입 차단 — users.email NOT NULL 제약 보호
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("OAUTH2_EMAIL_REQUIRED"));
        }
        // 같은 이메일로 이미 로컬 계정이 존재하면 소셜 가입 차단
        if (userRepository.existsByEmail(userInfo.getEmail())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("DUPLICATE_EMAIL"));
        }
    }
}
