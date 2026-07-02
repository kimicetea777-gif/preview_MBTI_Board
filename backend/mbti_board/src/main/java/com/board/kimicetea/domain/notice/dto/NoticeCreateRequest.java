package com.board.kimicetea.domain.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** 공지사항 생성 요청 DTO (ADMIN 전용) */
public record NoticeCreateRequest(

        @NotBlank @Size(max = 200) String title,

        @NotBlank String content,

        boolean isActive,

        int priority,

        Instant startsAt,

        Instant endsAt
) {
}
