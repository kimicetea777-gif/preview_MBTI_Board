package com.board.kimicetea.domain.comment.dto;

import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.user.enums.MbtiVerified;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 댓글 응답 DTO — 루트 댓글은 replies 포함, 대댓글은 replies = 빈 리스트
 * myReactionType: 인증 유저의 현재 리액션 (null = 없음, 1 = LIKE, -1 = DISLIKE)
 * isMine: 현재 요청자가 작성한 댓글이면 true (비회원 댓글은 항상 false)
 */
public record CommentResponse(
        Long id,
        String content,
        String authorNickname,
        String authorMbtiId,
        String mbtiVerified,
        int likeCount,
        int dislikeCount,
        Short myReactionType,
        Short debateStance,
        String voteOptionText,
        boolean isGuest,
        boolean isMine,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> replies
) {
    /** 목록 조회용 — replies + 유저 리액션 맵 + 요청자 userId + 투표 선택지 맵 전달 */
    public static CommentResponse from(final Comment comment, final List<Comment> replyEntities,
            final Map<Long, Short> reactionMap, final UUID userId, final Map<UUID, String> voteOptionMap) {
        return build(comment,
                replyEntities.stream().map(r -> from(r, reactionMap, userId, voteOptionMap)).toList(),
                reactionMap.getOrDefault(comment.getId(), null),
                userId,
                voteOptionMap);
    }

    /** 단건 반환용 (작성·수정) — 호출자가 항상 소유자이므로 userId 전달 */
    public static CommentResponse from(final Comment comment, final UUID userId) {
        return build(comment, List.of(), null, userId, Map.of());
    }

    private static CommentResponse from(final Comment comment, final Map<Long, Short> reactionMap,
            final UUID userId, final Map<UUID, String> voteOptionMap) {
        return build(comment, List.of(), reactionMap.getOrDefault(comment.getId(), null), userId, voteOptionMap);
    }

    private static CommentResponse build(final Comment comment, final List<CommentResponse> replies,
            final Short myReactionType, final UUID userId, final Map<UUID, String> voteOptionMap) {
        final boolean hasUser = comment.getUser() != null;
        final boolean isMine = userId != null && hasUser && comment.getUser().getId().equals(userId);
        final String voteOptionText = hasUser ? voteOptionMap.get(comment.getUser().getId()) : null;
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                hasUser ? comment.getUser().getNickname() : "(비회원)",
                comment.getAuthorMbti().getId(),
                hasUser ? comment.getUser().getMbtiVerified().name() : MbtiVerified.DEFAULT.name(),
                comment.getLikeCount(),
                comment.getDislikeCount(),
                myReactionType,
                comment.getDebateStance(),
                voteOptionText,
                comment.getGuestPassword() != null,
                isMine,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
