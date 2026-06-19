package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiQuestion;

/**
 * MBTI 검사 문항 응답 DTO (GET /api/mbti/test/questions)
 * 주의: isReverse는 프론트가 채점 시 점수 부호 반전에 사용
 */
public record MbtiQuestionResponse(
        String type,
        Long id,
        String category,
        String title,
        boolean isReverse
) implements QuestionResponse {

    public static MbtiQuestionResponse from(final MbtiQuestion question) {
        return new MbtiQuestionResponse(
                "MBTI",
                question.getId(),
                question.getCategory(),
                question.getTitle(),
                question.isReverse()
        );
    }
}
