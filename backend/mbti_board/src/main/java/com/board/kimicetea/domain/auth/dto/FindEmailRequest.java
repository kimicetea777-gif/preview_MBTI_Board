package com.board.kimicetea.domain.auth.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 이메일 찾기 요청 DTO — 전화번호 기반 조회
 */
public record FindEmailRequest(
        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = ValidationConstants.PHONE_REGEXP, message = ValidationConstants.PHONE_MESSAGE)
        String phone
) {
}
