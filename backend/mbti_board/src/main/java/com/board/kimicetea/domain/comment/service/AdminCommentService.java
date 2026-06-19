package com.board.kimicetea.domain.comment.service;

import com.board.kimicetea.domain.comment.dto.AdminCommentSummaryResponse;
import com.board.kimicetea.domain.comment.entity.Comment;
import com.board.kimicetea.domain.comment.repository.CommentRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 댓글 관리 비즈니스 로직 (목록 조회·숨김·복구)
 * 주의: 숨겨진 댓글 포함 모든 댓글을 대상으로 동작 — isVisible 필터 없음
 */
@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentRepository commentRepository;

    /** 댓글 전체 목록 조회 (숨겨진 댓글 포함) — postId·isVisible·authorNickname·sortDir 선택 필터 */
    @Transactional(readOnly = true)
    public Page<AdminCommentSummaryResponse> getComments(final Long postId, final Boolean isVisible,
            final String authorNickname, final String sortDir, final Pageable pageable) {
        return commentRepository.findAllCommentsForAdmin(postId, isVisible, authorNickname, sortDir, pageable)
                .map(AdminCommentSummaryResponse::from);
    }

    /** 댓글 가시성 변경 (숨김: false / 복구: true) */
    @Transactional
    public void changeCommentVisibility(final long commentId, final boolean visible) {
        final Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        comment.changeVisibility(visible);
    }
}
