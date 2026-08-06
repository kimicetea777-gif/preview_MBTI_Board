package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/** 내 프로필 조회 응답 DTO (GET /api/users/me) */
public record MyProfileResponse(
        UUID id,
        String email,
        String nickname,
        String profileImageUrl,
        String mbtiId,
        String mbtiVerified,
        String role,
        String state,
        Instant createdAt,
        Instant lastLoginAt
) {
    public static MyProfileResponse from(final User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getMbti().getId(),
                user.getMbtiVerified().name(),
                user.getRole().name(),
                user.getState().name(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
