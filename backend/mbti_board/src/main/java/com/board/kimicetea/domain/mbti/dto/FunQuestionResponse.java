package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.FunQuestion;

import java.util.List;

/**
 * FN(재미) 질문 응답 DTO — type="FUN"으로 직렬화
 */
public record FunQuestionResponse(
        String type,
        Long id,
        String title,
        List<FunOptionResponse> options
) implements QuestionResponse {

    public static FunQuestionResponse from(final FunQuestion question) {
        return new FunQuestionResponse(
                "FUN",
                question.getId(),
                question.getTitle(),
                question.getOptions().stream()
                        .map(FunOptionResponse::from)
                        .toList()
        );
    }
}
