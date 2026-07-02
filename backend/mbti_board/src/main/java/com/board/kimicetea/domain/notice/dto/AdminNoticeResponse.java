package com.board.kimicetea.domain.notice.dto;

import com.board.kimicetea.domain.notice.entity.Notice;

import java.time.Instant;

/** 어드민 공지사항 응답 DTO — 관리 필드(isActive·priority·기간) 포함 */
public record AdminNoticeResponse(
        Long id,
        String title,
        String content,
        boolean isActive,
        int priority,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminNoticeResponse from(final Notice notice) {
        return new AdminNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getIsActive(),
                notice.getPriority(),
                notice.getStartsAt(),
                notice.getEndsAt(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
