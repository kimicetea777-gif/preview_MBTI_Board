package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 게시글 목록 QueryDSL 커스텀 조회 인터페이스
 */
public interface PostRepositoryCustom {

    Page<Post> findPosts(String typeCode, PostSortType sort, String authorMbti, String targetMbti, Pageable pageable);

    /** 어드민 전용 — isVisible 필터 없음, 숨겨진 게시글 포함 조회 */
    Page<Post> findAllPostsForAdmin(String typeCode, Boolean isVisible, Pageable pageable);
}
