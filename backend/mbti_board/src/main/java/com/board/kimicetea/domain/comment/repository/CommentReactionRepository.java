package com.board.kimicetea.domain.comment.repository;

import com.board.kimicetea.domain.comment.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 댓글 리액션 CRUD
 * 주의: like_count·dislike_count는 DB 트리거가 관리 — JPA 직접 수정 금지
 */
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    /** 특정 댓글에 대한 특정 유저의 리액션 단건 조회 */
    Optional<CommentReaction> findByComment_IdAndUser_Id(Long commentId, UUID userId);

    /** 특정 유저의 여러 댓글 리액션 일괄 조회 — 댓글 목록 조회 시 myReactionType 구성용 */
    List<CommentReaction> findByUser_IdAndComment_IdIn(UUID userId, List<Long> commentIds);
}
