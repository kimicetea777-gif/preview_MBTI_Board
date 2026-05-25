package com.board.kimicetea.domain.comment.dto;

import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 댓글 응답 DTO — 루트 댓글은 replies 포함, 대댓글은 replies = 빈 리스트
 * myReactionType: 인증 유저의 현재 리액션 (null = 없음, 1 = LIKE, -1 = DISLIKE)
 */
public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        String authorMbtiId,
        String authorMbtiColor,
        String mbtiVerified,
        String mbtiVerifiedColor,
        int likeCount,
        int dislikeCount,
        Short myReactionType,
        Short debateStance,
        boolean isGuest,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> replies
) {
    /** 목록 조회용 — replies + 유저 리액션 맵 전달 */
    public static CommentResponse from(final Comment comment, final List<Comment> replyEntities,
            final Map<Long, Short> reactionMap) {
        return build(comment,
                replyEntities.stream().map(r -> from(r, reactionMap)).toList(),
                reactionMap.getOrDefault(comment.getId(), null));
    }

    /** 단건 반환용 (작성·수정) — myReactionType null */
    public static CommentResponse from(final Comment comment) {
        return build(comment, List.of(), null);
    }

    private static CommentResponse from(final Comment comment, final Map<Long, Short> reactionMap) {
        return build(comment, List.of(), reactionMap.getOrDefault(comment.getId(), null));
    }

    private static CommentResponse build(final Comment comment, final List<CommentResponse> replies,
            final Short myReactionType) {
        final boolean hasUser = comment.getUser() != null;
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                hasUser ? comment.getUser().getNickname() : "(비회원)",
                comment.getAuthorMbti().getId(),
                comment.getAuthorMbti().getColorCode(),
                hasUser ? comment.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                hasUser ? comment.getUser().getMbtiVerified().getColorCode() : MbtiVerified.DEFAULT.getColorCode(),
                comment.getLikeCount(),
                comment.getDislikeCount(),
                myReactionType,
                comment.getDebateStance(),
                comment.getGuestPassword() != null,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
