package com.board.kimicetea.domain.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security Principal 구현체
 * 주의: password는 OAuth2 사용자에게 null일 수 있음 (소셜 로그인 전용 계정)
 */
public record CustomUserDetails(
        UUID userId,
        String email,
        String password,
        String role,
        String deviceId) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
