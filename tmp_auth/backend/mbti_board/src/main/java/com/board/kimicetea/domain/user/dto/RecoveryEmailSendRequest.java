package com.board.kimicetea.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 복구 이메일 등록/변경 — 인증 링크 발송 요청 DTO */
public record RecoveryEmailSendRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
