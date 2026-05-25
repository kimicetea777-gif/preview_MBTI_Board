package com.board.kimicetea.domain.comment.service;

import com.board.kimicetea.domain.comment.dto.CommentReactionResponse;
import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.comment.entity.CommentReaction;
import com.board.kimicetea.domain.comment.repository.CommentReactionRepository;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 댓글 리액션(좋아요/싫어요) 비즈니스 로직 — 회원 전용
 * 주의: like_count·dislike_count는 DB 트리거가 갱신하므로 EntityManager.flush()+refresh()로 최신값 조회
 */
@Service
@RequiredArgsConstructor
public class CommentReactionService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 리액션 토글 — POST /api/comments/{id}/reactions
     * - 리액션 없음 → 신규 생성
     * - 같은 타입 존재 → 취소(삭제)
     * - 다른 타입 존재 → 타입 전환(UPDATE)
     * 주의: reactionType은 1(LIKE) 또는 -1(DISLIKE)만 허용
     */
    @Transactional
    public CommentReactionResponse toggleReaction(final Long commentId, final Short reactionType,
            final UUID userId) {
        if (reactionType != 1 && reactionType != -1) {
            throw new CustomException(ErrorCode.INVALID_REACTION_TYPE);
        }

        final Comment comment = findCommentOrThrow(commentId);
        final Optional<CommentReaction> existing = reactionRepository.findByComment_IdAndUser_Id(commentId, userId);

        final Short resultType = existing.isPresent()
                ? handleExistingReaction(existing.get(), reactionType)
                : createReaction(comment, userId, reactionType);

        return buildResponse(commentId, resultType, comment);
    }

    /**
     * 리액션 취소 — DELETE /api/comments/{id}/reactions
     * 주의: 리액션이 없어도 200 반환 (멱등 처리)
     */
    @Transactional
    public CommentReactionResponse cancelReaction(final Long commentId, final UUID userId) {
        final Comment comment = findCommentOrThrow(commentId);
        reactionRepository.findByComment_IdAndUser_Id(commentId, userId)
                .ifPresent(reactionRepository::delete);
        return buildResponse(commentId, null, comment);
    }

    /**
     * flush로 DB 트리거 실행 후 Comment 엔티티 refresh → 최신 likeCount·dislikeCount 확보
     */
    private CommentReactionResponse buildResponse(final Long commentId, final Short resultType,
            final Comment comment) {
        entityManager.flush();
        entityManager.refresh(comment);
        return new CommentReactionResponse(commentId, resultType, comment.getLikeCount(), comment.getDislikeCount());
    }

    /** 같은 타입 → 취소(null), 다른 타입 → 전환 후 새 타입 반환 */
    private Short handleExistingReaction(final CommentReaction reaction, final Short reactionType) {
        if (reaction.getReactionType().equals(reactionType)) {
            reactionRepository.delete(reaction);
            return null;
        }
        reaction.updateReactionType(reactionType);
        return reactionType;
    }

    /** 최초 리액션 생성 후 reactionType 반환 */
    private Short createReaction(final Comment comment, final UUID userId, final Short reactionType) {
        // JWT 인증을 통과한 유저는 반드시 존재 — SELECT 없이 프록시로 FK만 설정
        final User user = userRepository.getReferenceById(userId);
        reactionRepository.save(CommentReaction.builder()
                .comment(comment)
                .user(user)
                .reactionType(reactionType)
                .build());
        return reactionType;
    }

    private Comment findCommentOrThrow(final Long commentId) {
        return commentRepository.findByIdAndIsVisibleTrue(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }
}
