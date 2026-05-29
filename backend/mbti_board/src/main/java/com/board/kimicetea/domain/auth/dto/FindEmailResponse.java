package com.board.kimicetea.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 이메일 찾기 응답 DTO
 * [주의] maskedEmail: @앞 2자 노출, 나머지 *** 처리 (예: ab***@gmail.com)
 */
public record FindEmailResponse(
        String maskedEmail,
        @JsonProperty("isSocialOnly") boolean isSocialOnly
) {
}
