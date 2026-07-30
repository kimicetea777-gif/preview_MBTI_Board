package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 이메일 인증 토큰 확인 요청 DTO */
public record EmailVerifyConfirmRequest(
        @NotBlank(message = "인증 토큰을 입력해 주세요.")
        String token
) {}
