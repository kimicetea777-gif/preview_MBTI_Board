package com.board.kimicetea.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원 탈퇴 요청 DTO (DELETE /api/users/me)
 * 주의: OTP 발송 후 3분 이내에 수신한 코드를 입력해야 함
 */
public record WithdrawalRequest(
        @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
        String code
) {}
