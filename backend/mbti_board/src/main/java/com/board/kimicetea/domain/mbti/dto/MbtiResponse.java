package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;
import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * MBTI 메타데이터 응답 DTO (GET /api/mbti, GET /api/mbti/{id})
 * 주의: createdAt/updatedAt 등 내부 관리 필드는 노출하지 않음
 */
public record MbtiResponse(
        String id,
        String iconUrl,
        String colorCode,
        String summary,
        @JsonRawValue String description
) {
    public static MbtiResponse from(final MbtiMetadata mbti) {
        return new MbtiResponse(
                mbti.getId(),
                mbti.getIconUrl(),
                mbti.getColorCode(),
                mbti.getSummary(),
                mbti.getDescription()
        );
    }
}
