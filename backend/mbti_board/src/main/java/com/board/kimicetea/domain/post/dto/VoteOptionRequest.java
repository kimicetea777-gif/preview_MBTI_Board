package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 작성 요청 DTO
 * 주의: mbtiTarget은 MBTI 투표일 때만 전달 — 자유 투표는 null
 */
public record VoteOptionRequest(

        @NotBlank
        @Size(max = 100)
        String optionText,

        // MBTI 투표 선택지일 때 MBTI ID ("INFP" 등), 자유 선택지는 null
        String mbtiTarget
) {}
