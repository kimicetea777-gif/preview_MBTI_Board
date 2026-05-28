package com.board.kimicetea.domain.comment.repository;

import com.board.kimicetea.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 댓글 목록 QueryDSL 커스텀 조회 인터페이스
 */
public interface CommentRepositoryCustom {

    /** 어드민 전용 — isVisible 필터 없음, 숨겨진 댓글 포함 조회 */
    Page<Comment> findAllCommentsForAdmin(Long postId, Boolean isVisible, Pageable pageable);
}
