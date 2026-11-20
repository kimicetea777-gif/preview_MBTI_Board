package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.PostConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 투표 게시글 작성 요청 DTO (POST /api/posts/vote 전용 — 회원 전용)
 * 주의:
 * - voteOptions 최소 2개, 최대 PostConstants.VOTE_OPTION_MAX개(MBTI 16종 대응) — Bean Validation으로 검증
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

        @NotNull(message = "투표 게시글은 최소 2개 이상의 선택지가 필요합니다.")
        @Size(min = 2, max = PostConstants.VOTE_OPTION_MAX,
                message = "투표 선택지는 2개 이상 " + PostConstants.VOTE_OPTION_MAX + "개 이하여야 합니다.")
        List<@Valid VoteOptionRequest> voteOptions,

        Instant endsAt
) {}
