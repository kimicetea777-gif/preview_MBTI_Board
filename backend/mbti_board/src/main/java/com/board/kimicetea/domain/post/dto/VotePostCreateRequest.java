package com.board.kimicetea.domain.post.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 투표 게시글 작성 요청 DTO (POST /api/posts/vote 전용 — 회원 전용)
 * 주의:
 * - voteOptions 최소 2개, 최대 16개(MBTI 16종 대응) — 서비스에서 검증
 * - endsAt null이면 무기한
 */
public record VotePostCreateRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 50_000)
        String content,

        // null 또는 "ALL" 이면 전체 대상 — 특정 MBTI ID("INFP" 등) 지정 시 해당 MBTI 타겟
        String targetMbti,

        @Size(min = 2, max = 16)
        List<@Valid VoteOptionRequest> voteOptions,

        Instant endsAt
) {}
