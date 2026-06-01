package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 DTO (PATCH /api/users/me/password)
 * 주의: 소셜 전용 계정은 서비스 레이어에서 차단
 */
public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 10, message = "비밀번호는 영문+숫자 포함 10자 이상이어야 합니다.")
        @Pattern(regexp = ValidationConstants.PASSWORD_REGEXP, message = ValidationConstants.PASSWORD_MESSAGE)
        String newPassword
) {}
