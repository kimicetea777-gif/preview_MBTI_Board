package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 찾기 요청 DTO
 */
public record FindEmailRequest(
        @NotBlank(message = "닉네임을 입력해 주세요.")
        String nickname
) {
}
