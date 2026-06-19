package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 어드민 FUN 선택지 텍스트 수정 요청 DTO — AdminFunQuestionUpdateRequest 내부에 포함
 */
public record AdminFunOptionUpdateRequest(
        @NotNull Long id,
        @NotBlank @Size(max = 100) String optionText
) {}
