package com.board.kimicetea.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/** 복구 이메일 인증 링크의 token 확인 요청 DTO */
public record RecoveryEmailConfirmRequest(
        @NotBlank(message = "토큰이 필요합니다.")
        String token
) {
}
