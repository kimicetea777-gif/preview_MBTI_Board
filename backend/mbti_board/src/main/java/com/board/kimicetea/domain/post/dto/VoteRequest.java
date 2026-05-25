package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 투표 참여 요청 DTO — 회원 전용
 */
public record VoteRequest(

        @NotNull
        Long optionId
) {}
