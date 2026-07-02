package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.global.security.oauth2.userinfo.OAuth2UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 약관 동의 전 신규 소셜 가입자용 임시 Principal
 * 주의: User가 아직 DB에 없는 상태 — OAuth2UserPrincipal(User 보유) 대신 프로필 정보만 담아
 *       OAuth2AuthenticationSuccessHandler에 전달 (동의 완료 API에서 실제 User 생성)
 */
public class PendingOAuth2SignupPrincipal implements OAuth2User {

    private final String provider;
    private final OAuth2UserInfo userInfo;

    public PendingOAuth2SignupPrincipal(final String provider, final OAuth2UserInfo userInfo) {
        this.provider = provider;
        this.userInfo = userInfo;
    }

    public String getProvider() {
        return provider;
    }

    public OAuth2UserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return provider + ":" + userInfo.getProviderId();
    }
}
