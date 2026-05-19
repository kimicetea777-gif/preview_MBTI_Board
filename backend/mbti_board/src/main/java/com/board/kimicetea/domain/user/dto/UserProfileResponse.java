package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.entity.User;

import java.util.UUID;

/**
 * 타인 공개 프로필 응답 DTO (GET /api/users/{id})
 * 주의: 이메일·상태 등 민감 정보 제외
 */
public record UserProfileResponse(
        UUID id,
        String nickname,
        String profileImageUrl,
        String mbtiId,
        String mbtiColorCode,
        String mbtiVerified,
        String mbtiVerifiedColorCode
) {
    public static UserProfileResponse from(final User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getMbti().getId(),
                user.getMbti().getColorCode(),
                user.getMbtiVerified().name(),
                user.getMbtiVerified().getColorCode()
        );
    }
}
