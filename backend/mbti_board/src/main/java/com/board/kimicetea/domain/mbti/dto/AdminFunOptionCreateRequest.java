package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 어드민 FUN 선택지 생성 요청 DTO — AdminFunQuestionCreateRequest 내부에 포함
 * 주의: displayOrder는 요청받지 않고 options 리스트 순서로 서비스에서 자동 부여
 */
public record AdminFunOptionCreateRequest(
        @NotBlank @Size(max = 100) String optionText
) {}
