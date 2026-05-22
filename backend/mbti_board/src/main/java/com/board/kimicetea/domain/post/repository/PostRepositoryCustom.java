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
}
