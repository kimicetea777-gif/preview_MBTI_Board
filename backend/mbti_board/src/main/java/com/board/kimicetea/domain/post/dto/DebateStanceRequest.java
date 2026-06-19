package com.board.kimicetea.domain.post.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 토론 진영 투표 요청 DTO
 */
public record DebateStanceRequest(
        @NotNull
        Short stance
) {}
