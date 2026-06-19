package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 어드민 MBTI 문항 노출 여부 변경 요청 DTO
 */
public record AdminMbtiQuestionVisibilityRequest(
        @NotNull Boolean visible
) {}
