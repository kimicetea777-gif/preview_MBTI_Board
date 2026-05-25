package com.board.kimicetea.domain.post.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 게시글 작성 요청 DTO
 * 주의:
 * - guestPassword는 비회원 전용 — 서비스에서 인증 여부에 따라 필수 검증
 * - voteOptions는 VOTE 타입 전용 — 최소 2개 이상, 서비스에서 검증
 * - endsAt은 VOTE·DEBATE 공통 — null이면 무기한
 */
public record PostCreateRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 50_000)
        String content,

        @NotBlank
        @Size(max = 10)
        String type,

        // BCrypt 72바이트 한계로 최대 72자 제한
        @Size(min = 4, max = 72)
        String guestPassword,

        // null 또는 "ALL" 이면 전체 대상 — 특정 MBTI ID("INFP" 등) 지정 시 해당 MBTI 타겟
        String targetMbti,

        // VOTE 타입 전용 — 최소 2개, 최대 10개, 서비스에서 검증
        @Size(min = 2, max = 10)
        List<@Valid VoteOptionRequest> voteOptions,

        // VOTE·DEBATE 공통 마감일 — null이면 무기한
        Instant endsAt
) {}
