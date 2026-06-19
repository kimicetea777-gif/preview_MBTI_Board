package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * FN 질문 응답 제출 요청 DTO (POST /api/mbti/fun/responses)
 * 주의: 인증 불필요 — 비회원 포함
 */
public record FunResponseSubmitRequest(
        @NotNull
        @Pattern(regexp = ValidationConstants.MBTI_REGEXP, message = ValidationConstants.MBTI_MESSAGE)
        String mbtiId,
        @NotEmpty @Size(max = 10) List<@NotNull Long> optionIds
) {}
