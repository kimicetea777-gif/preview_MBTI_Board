package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 수정 요청 DTO
 * 주의: guestPassword는 비회원 전용 — 서비스에서 회원/비회원 분기 처리
 */
public record PostUpdateRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 50_000)
        String content,

        @Size(min = 4, max = 72)
        String guestPassword
) {}
