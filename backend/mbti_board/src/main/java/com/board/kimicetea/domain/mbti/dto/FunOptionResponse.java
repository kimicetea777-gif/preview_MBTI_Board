package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.FunQuestionOption;

/**
 * FN 질문 선택지 응답 DTO
 */
public record FunOptionResponse(
        Long id,
        String optionText,
        int displayOrder
) {
    public static FunOptionResponse from(final FunQuestionOption option) {
        return new FunOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getDisplayOrder()
        );
    }
}
