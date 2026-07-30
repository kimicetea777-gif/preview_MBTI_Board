package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 어드민 MBTI 문항 생성 요청 DTO
 * 주의: category는 EI·SN·TF·JP 중 하나 — testVersion은 요청에서 받지 않고 서비스에서 고정값("v1.0")으로 채워짐
 */
public record AdminMbtiQuestionCreateRequest(

        @NotBlank
        @Size(max = 300)
        String title,

        @NotBlank
        @Pattern(regexp = "EI|SN|TF|JP", message = "category는 EI, SN, TF, JP 중 하나여야 합니다.")
        String category,

        @NotNull Boolean isReverse
) {}
