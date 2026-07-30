package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 찾기 요청 DTO — 인증된 복구 이메일 기반 조회
 */
public record FindEmailRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String recoveryEmail
) {
}
