package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 어드민 MBTI 문항 수정 요청 DTO
 * 주의: category는 EI·SN·TF·JP 중 하나, testVersion은 변경 불가
 */
public record AdminMbtiQuestionUpdateRequest(

        @NotBlank
        @Size(max = 300)
        String title,

        @NotBlank
        @Pattern(regexp = "EI|SN|TF|JP", message = "category는 EI, SN, TF, JP 중 하나여야 합니다.")
        String category,

        @NotNull Boolean isReverse
) {}
