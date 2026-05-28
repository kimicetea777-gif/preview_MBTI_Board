package com.board.kimicetea.domain.mbti.dto;

/**
 * MBTI 검사 결과 응답 DTO (POST /api/mbti/test/submit)
 */
public record MbtiTestResultResponse(
        String mbtiId,
        short eiScore,
        short snScore,
        short tfScore,
        short jpScore
) {}
