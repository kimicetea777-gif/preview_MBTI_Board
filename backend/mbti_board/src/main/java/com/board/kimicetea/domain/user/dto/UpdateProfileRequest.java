package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청 DTO (PATCH /api/users/me)
 * 주의: 모든 필드는 선택적 — null인 필드는 수정하지 않음
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 12, message = "별명은 2~12자여야 합니다.")
        @Pattern(regexp = ValidationConstants.NICKNAME_REGEXP, message = ValidationConstants.NICKNAME_MESSAGE)
        String nickname,

        String profileImageUrl,

        @Pattern(regexp = ValidationConstants.MBTI_OR_NONE_REGEXP, message = ValidationConstants.MBTI_MESSAGE)
        String mbtiId
) {}
