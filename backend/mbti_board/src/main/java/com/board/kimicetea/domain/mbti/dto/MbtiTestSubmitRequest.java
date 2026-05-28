package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * MBTI 검사 결과 제출 요청 DTO (POST /api/mbti/test/submit)
 * 주의: 점수 계산은 프론트에서 수행, 서버는 점수 범위 검증 후 MBTI를 독립적으로 재도출
 */
public record MbtiTestSubmitRequest(
        @NotNull @Min(-16) @Max(16) Short eiScore,
        @NotNull @Min(-16) @Max(16) Short snScore,
        @NotNull @Min(-16) @Max(16) Short tfScore,
        @NotNull @Min(-16) @Max(16) Short jpScore
) {}
