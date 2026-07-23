package com.board.kimicetea.domain.post.service;

import com.board.kimicetea.domain.post.dto.AdminPostSummaryResponse;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
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
 * 어드민 게시글 관리 비즈니스 로직 (목록/단건 조회·숨김·복구)
 * 주의: 숨겨진 게시글 포함 모든 게시글을 대상으로 동작 — isVisible 필터 없음
 */
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final PostService postService;

    /** 게시글 전체 목록 조회 (숨겨진 게시글 포함) — typeCode·isVisible·authorNickname 필터, sortDir 정렬 */
    @Transactional(readOnly = true)
    public Page<AdminPostSummaryResponse> getPosts(final String typeCode, final Boolean isVisible,
            final String authorNickname, final String sortDir, final Pageable pageable) {
        return postRepository.findAllPostsForAdmin(typeCode, isVisible, authorNickname, sortDir, pageable)
                .map(AdminPostSummaryResponse::from);
    }

    /**
     * 게시글 단건 조회 — 숨겨진 게시글도 조회 가능 (모더레이션 확인용)
     * 주의: 실제 조회가 아니므로 viewCount는 증가시키지 않음
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPost(final long postId) {
        final Post post = postRepository.findWithDetailsByIdForAdmin(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return postService.buildDetailResponse(post, null);
    }

    /** 게시글 가시성 변경 (숨김: false / 복구: true) */
    @Transactional
    public void changePostVisibility(final long postId, final boolean visible) {
        final Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.changeVisibility(visible);
    }

    /** 게시글 영구 삭제 — CASCADE로 댓글 전체 함께 삭제 */
    @Transactional
    public void deletePost(final long postId) {
        final Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        postRepository.delete(post);
    }
}
