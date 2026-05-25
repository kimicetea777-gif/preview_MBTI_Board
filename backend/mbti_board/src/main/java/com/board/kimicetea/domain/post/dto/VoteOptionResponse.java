package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.VotePostOption;

/**
 * 투표 선택지 응답 DTO
 * 주의: voteCount는 DB 트리거 관리 값 — 직접 수정 금지
 */
public record VoteOptionResponse(
        Long id,
        String optionText,
        String mbtiTargetId,
        int voteCount
) {
    public static VoteOptionResponse from(final VotePostOption option) {
        return new VoteOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getMbtiTarget() != null ? option.getMbtiTarget().getId() : null,
                option.getVoteCount()
        );
    }
}
