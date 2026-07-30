package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 찾기 OTP 확인 요청 DTO — 발송된 6자리 코드 검증 후 마스킹된 이메일 조회
 */
public record FindEmailConfirmRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String recoveryEmail,

        @NotBlank(message = "인증 코드를 입력해 주세요.")
        String otp
) {
}
