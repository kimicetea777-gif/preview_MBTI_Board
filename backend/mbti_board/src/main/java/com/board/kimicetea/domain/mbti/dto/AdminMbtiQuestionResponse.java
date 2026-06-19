package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiQuestion;

import java.time.Instant;

/**
 * 어드민 MBTI 문항 응답 DTO (GET /api/admin/mbti/questions 전용)
 * 주의: 공개 엔드포인트(GET /api/mbti/test/questions)에는 사용 금지 — isVisible·updatedAt 포함
 */
public record AdminMbtiQuestionResponse(
        Long id,
        String category,
        String title,
        boolean isReverse,
        boolean isVisible,
        Instant updatedAt
) {
    public static AdminMbtiQuestionResponse from(final MbtiQuestion q) {
        return new AdminMbtiQuestionResponse(
                q.getId(),
                q.getCategory(),
                q.getTitle(),
                q.isReverse(),
                q.isVisible(),
                q.getUpdatedAt()
        );
    }
}
