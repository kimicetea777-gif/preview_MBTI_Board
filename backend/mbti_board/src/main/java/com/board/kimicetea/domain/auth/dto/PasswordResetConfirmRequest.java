package com.board.kimicetea.domain.auth.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 확인 요청 DTO
 */
public record PasswordResetConfirmRequest(
        @NotBlank(message = "토큰을 입력해 주세요.")
        String token,

        @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Size(min = 10, message = "비밀번호는 10자 이상이어야 합니다.")
        @Pattern(regexp = ValidationConstants.PASSWORD_REGEXP, message = ValidationConstants.PASSWORD_MESSAGE)
        String newPassword
) {
}
