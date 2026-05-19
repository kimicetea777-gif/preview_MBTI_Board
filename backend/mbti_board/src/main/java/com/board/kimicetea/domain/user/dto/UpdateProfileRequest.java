package com.board.kimicetea.domain.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청 DTO (PATCH /api/users/me)
 * 주의: 모든 필드는 선택적 — null인 필드는 수정하지 않음
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
        String nickname,

        String profileImageUrl,

        String mbtiId
) {}
