package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.FunQuestion;

import java.time.Instant;
import java.util.List;

/**
 * 어드민 FUN 문항 응답 DTO (GET /api/admin/mbti/fun-questions 전용)
 * 주의: isVisible·updatedAt·options 포함 — 공개 엔드포인트에는 사용 금지
 */
public record AdminFunQuestionResponse(
        Long id,
        String title,
        List<AdminFunOptionResponse> options,
        boolean isVisible,
        Instant updatedAt
) {
    public static AdminFunQuestionResponse from(final FunQuestion q) {
        return new AdminFunQuestionResponse(
                q.getId(),
                q.getTitle(),
                q.getOptions().stream().map(AdminFunOptionResponse::from).toList(),
                q.isVisible(),
                q.getUpdatedAt()
        );
    }
}
