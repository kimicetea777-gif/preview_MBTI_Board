package com.board.kimicetea.global.security.oauth2;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AS(Authorization Server) access token의 {@code app_role} claim(client=TypeButter 전용 자유
 * 문자열, AS 관리자가 {@code /admin/membership}에서 세팅)을 로컬 {@link User#getRole()}에 동기화한다.
 * <p>
 * AS 쪽은 client마다 role 어휘가 달라 값 검증을 하지 않으므로, 여기서 {@link UserRole} enum 이름과
 * 정확히 일치할 때만 반영한다 — claim이 없거나(아직 미배정) 이 enum에 없는 값이면 로컬 role을 그대로
 * 둔다. 그래서 AS가 app_role을 세팅하지 않은 유저는 기존처럼 {@code AdminUserService.updateUser()}로만
 * 로컬 승격되는 경로가 그대로 유지된다 — app_role은 "있으면 우선 적용", 없으면 로컬이 계속 source of truth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppRoleSyncService {

    private static final String APP_ROLE_CLAIM = "app_role";

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    /** @return 동기화 이후 실제로 적용해야 할 role(반영 여부와 무관하게 항상 유효한 값을 돌려준다) */
    @Transactional
    public UserRole syncFromAccessToken(final User user, final String accessTokenValue) {
        final String appRole = decodeAppRole(accessTokenValue);
        if (appRole == null) {
            return user.getRole();
        }

        final UserRole newRole;
        try {
            newRole = UserRole.valueOf(appRole);
        } catch (final IllegalArgumentException e) {
            log.warn("알 수 없는 app_role 값 — 로컬 role 유지 (userId={}, appRole={})", user.getId(), appRole);
            return user.getRole();
        }

        if (newRole != user.getRole()) {
            log.info("app_role에 따라 로컬 role 동기화 (userId={}, {} -> {})", user.getId(), user.getRole(), newRole);
            user.changeRole(newRole);
            userRepository.save(user);
        }
        return newRole;
    }

    private String decodeAppRole(final String accessTokenValue) {
        try {
            final Jwt jwt = jwtDecoder.decode(accessTokenValue);
            return jwt.getClaimAsString(APP_ROLE_CLAIM);
        } catch (final JwtException e) {
            log.warn("AS access token 디코딩 실패 — app_role 동기화 건너뜀", e);
            return null;
        }
    }
}
