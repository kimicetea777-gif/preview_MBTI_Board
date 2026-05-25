package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.VotePost;
import com.board.kimicetea.domain.post.entity.VotePostHistory;

import java.time.Instant;
import java.util.List;

/**
 * 투표 결과 응답 DTO — 선택지별 득표수 + 본인 선택 옵션 + 마감 여부
 */
public record VoteResultResponse(
        List<VoteOptionResponse> options,
        Long myVotedOptionId,
        boolean isEnded,
        Instant endsAt
) {
    /**
     * @param votePost options이 JOIN FETCH된 VotePost
     * @param history  본인 투표 내역 — 비로그인 또는 미투표 시 null
     */
    public static VoteResultResponse from(final VotePost votePost, final VotePostHistory history) {
        final List<VoteOptionResponse> options = votePost.getOptions().stream()
                .map(VoteOptionResponse::from)
                .toList();

        final Long myVotedOptionId = (history != null) ? history.getVotePostOption().getId() : null;

        final boolean isEnded = votePost.getEndsAt() != null
                && Instant.now().isAfter(votePost.getEndsAt());

        return new VoteResultResponse(options, myVotedOptionId, isEnded, votePost.getEndsAt());
    }
}
