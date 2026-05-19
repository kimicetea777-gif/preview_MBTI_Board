package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * 내 프로필 조회 응답 DTO (GET /api/users/me)
 * 주의: 소셜 전용 계정 여부(isSocialOnly)로 프론트 비밀번호 변경 버튼 노출 제어
 */
public record MyProfileResponse(
        UUID id,
        String email,
        String nickname,
        String profileImageUrl,
        String mbtiId,
        String mbtiColorCode,
        String mbtiVerified,
        String mbtiVerifiedColorCode,
        String role,
        String state,
        boolean isSocialOnly,
        Instant createdAt
) {
    public static MyProfileResponse from(final User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getMbti().getId(),
                user.getMbti().getColorCode(),
                user.getMbtiVerified().name(),
                user.getMbtiVerified().getColorCode(),
                user.getRole().name(),
                user.getState().name(),
                user.getSocialProvider() != null,
                user.getCreatedAt()
        );
    }
}
