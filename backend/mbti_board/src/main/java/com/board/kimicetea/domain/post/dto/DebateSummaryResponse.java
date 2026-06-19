package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.DebatePost;

import java.time.Instant;

/**
 * 토론 게시글 요약 정보 응답 DTO
 * 역할: proLabel/conLabel, 찬반 카운트, 마감 여부, 현재 사용자의 진영 선택을 클라이언트에 전달
 */
public record DebateSummaryResponse(
        String proLabel,
        String conLabel,
        int proCount,
        int conCount,
        boolean isClosed,
        Instant endsAt,
        Short myStance
) {
    /** 비로그인 또는 stance 조회 불필요한 맥락(목록 조회 등)에서 사용 */
    public static DebateSummaryResponse from(DebatePost debatePost) {
        return from(debatePost, null);
    }

    /** 상세 조회 시 현재 사용자의 투표 내역 포함 */
    public static DebateSummaryResponse from(DebatePost debatePost, Short myStance) {
        final boolean closed = debatePost.getEndsAt() != null
                && debatePost.getEndsAt().isBefore(Instant.now());
        return new DebateSummaryResponse(
                debatePost.getProLabel(),
                debatePost.getConLabel(),
                debatePost.getProCount(),
                debatePost.getConCount(),
                closed,
                debatePost.getEndsAt(),
                myStance
        );
    }
}
