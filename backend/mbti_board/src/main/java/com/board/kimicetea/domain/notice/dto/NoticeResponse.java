package com.board.kimicetea.domain.notice.dto;

import com.board.kimicetea.domain.notice.entity.Notice;

import java.time.Instant;

/** 공개 공지사항 응답 DTO — is_active 등 관리 필드 미노출 */
public record NoticeResponse(
        Long id,
        String title,
        String content,
        Instant createdAt
) {
    public static NoticeResponse from(final Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }
}
