package com.board.kimicetea.domain.post.dto;

import com.board.kimicetea.domain.post.entity.DebatePost;

import java.time.Instant;

/**
 * 토론 게시글 요약 정보 응답 DTO
 * 역할: proLabel/conLabel, 찬반 카운트, 마감 여부를 클라이언트에 전달
 */
public record DebateSummaryResponse(
        String proLabel,
        String conLabel,
        int proCount,
        int conCount,
        boolean isClosed,
        Instant endsAt
) {
    public static DebateSummaryResponse from(DebatePost debatePost) {
        boolean closed = debatePost.getEndsAt() != null
                && debatePost.getEndsAt().isBefore(Instant.now());
        return new DebateSummaryResponse(
                debatePost.getProLabel(),
                debatePost.getConLabel(),
                debatePost.getProCount(),
                debatePost.getConCount(),
                closed,
                debatePost.getEndsAt()
        );
    }
}
