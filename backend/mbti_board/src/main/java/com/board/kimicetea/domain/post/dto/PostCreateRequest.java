package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 작성 요청 DTO
 * 주의: guestPassword는 비회원 전용 — 서비스에서 인증 여부에 따라 필수 검증
 */
public record PostCreateRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        String content,

        @NotBlank
        String type,

        String guestPassword
) {}
