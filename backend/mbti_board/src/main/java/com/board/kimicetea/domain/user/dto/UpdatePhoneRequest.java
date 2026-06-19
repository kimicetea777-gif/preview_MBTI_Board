package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 전화번호 등록/변경 요청 DTO (PATCH /api/users/me/phone)
 * 주의: 백엔드에서 HMAC 해싱 후 저장 — 원본은 보관하지 않음
 */
public record UpdatePhoneRequest(
        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = ValidationConstants.PHONE_REGEXP, message = ValidationConstants.PHONE_MESSAGE)
        String phone
) {}
