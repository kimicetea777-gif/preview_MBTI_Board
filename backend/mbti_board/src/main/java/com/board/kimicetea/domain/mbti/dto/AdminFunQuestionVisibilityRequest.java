package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 어드민 FUN 문항 노출 여부 변경 요청 DTO
 * 주의: Boolean 박싱 타입 사용 — @NotNull이 primitive boolean에는 적용되지 않음
 */
public record AdminFunQuestionVisibilityRequest(@NotNull Boolean visible) {}
