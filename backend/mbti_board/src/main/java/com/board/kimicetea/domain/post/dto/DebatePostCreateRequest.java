package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 토론 게시글 작성 요청 DTO (POST /api/posts/debate 전용 — 회원 전용)
 * 주의:
 * - proLabel/conLabel null이면 기본값("찬성"/"반대") 사용
 * - endsAt null이면 무기한
 */
public record DebatePostCreateRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 50_000)
        String content,

        // null 또는 "ALL" 이면 전체 대상 — 특정 MBTI ID("INFP" 등) 지정 시 해당 MBTI 타겟
        String targetMbti,

        // null이면 무기한
        Instant endsAt,

        // null이면 기본값("찬성") 사용
        @Size(max = 50)
        String proLabel,

        // null이면 기본값("반대") 사용
        @Size(max = 50)
        String conLabel
) {}
