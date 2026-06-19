package com.board.kimicetea.domain.mbti.dto;

import com.board.kimicetea.domain.mbti.entity.MbtiMetadata;

/**
 * MBTI 메타데이터 응답 DTO — 표시 데이터는 프론트 정적, id만 반환
 */
public record MbtiResponse(String id) {
    public static MbtiResponse from(final MbtiMetadata mbti) {
        return new MbtiResponse(mbti.getId());
    }
}
