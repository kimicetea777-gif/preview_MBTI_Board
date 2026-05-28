package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;

import java.time.Instant;

/**
 * MBTI 검사 이력 응답 DTO (GET /api/users/me/mbti/history)
 */
public record MbtiTestHistoryResponse(
        Long id,
        String mbtiId,
        short eiScore,
        short snScore,
        short tfScore,
        short jpScore,
        String testVersion,
        Instant createdAt
) {
    public static MbtiTestHistoryResponse from(final MbtiTestHistory history) {
        return new MbtiTestHistoryResponse(
                history.getId(),
                history.getMbti().getId(),
                history.getEiScore(),
                history.getSnScore(),
                history.getTfScore(),
                history.getJpScore(),
                history.getTestVersion(),
                history.getCreatedAt()
        );
    }
}
