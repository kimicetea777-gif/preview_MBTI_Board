package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.FunQuestionOption;

/**
 * 어드민 FUN 선택지 응답 DTO — AdminFunQuestionResponse 내부에 포함
 */
public record AdminFunOptionResponse(
        Long id,
        String optionText,
        short displayOrder
) {
    public static AdminFunOptionResponse from(final FunQuestionOption o) {
        return new AdminFunOptionResponse(o.getId(), o.getOptionText(), o.getDisplayOrder());
    }
}
