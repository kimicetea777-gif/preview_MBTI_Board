package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 유저 목록 응답 DTO — 이메일·상태·역할 등 민감 정보 포함
 * 주의: ADMIN 권한 전용 응답이므로 일반 유저 API에 재사용 금지
 */
public record AdminUserSummaryResponse(
        UUID id,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String state,
        String mbtiId,
        String mbtiVerified,
        boolean isSocialLogin,
        boolean hasRecoveryEmail,
        Instant lastLoginAt,
        Instant updatedAt,
        Instant createdAt
) {

    public static AdminUserSummaryResponse from(final User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                user.getState().name(),
                user.getMbti().getId(),
                user.getMbtiVerified().name(),
                user.getSocialProvider() != null,
                user.getRecoveryEmail() != null,
                user.getLastLoginAt(),
                user.getUpdatedAt(),
                user.getCreatedAt()
        );
    }
}
