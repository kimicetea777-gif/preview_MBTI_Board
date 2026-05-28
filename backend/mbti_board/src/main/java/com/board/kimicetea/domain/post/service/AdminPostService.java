package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.post.dto.AdminPostSummaryResponse;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.repository.PostRepository;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 게시글 관리 비즈니스 로직 (목록 조회·숨김·복구)
 * 주의: 숨겨진 게시글 포함 모든 게시글을 대상으로 동작 — isVisible 필터 없음
 */
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;

    /** 게시글 전체 목록 조회 (숨겨진 게시글 포함) — typeCode·isVisible 선택 필터 */
    @Transactional(readOnly = true)
    public Page<AdminPostSummaryResponse> getPosts(final String typeCode, final Boolean isVisible,
            final Pageable pageable) {
        return postRepository.findAllPostsForAdmin(typeCode, isVisible, pageable)
                .map(AdminPostSummaryResponse::from);
    }

    /** 게시글 가시성 변경 (숨김: false / 복구: true) */
    @Transactional
    public void changePostVisibility(final long postId, final boolean visible) {
        final Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.changeVisibility(visible);
    }
}
