package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * 게시글 목록 QueryDSL 커스텀 조회 인터페이스
 */
public interface PostRepositoryCustom {

    /** typeCode null 이면 전체 유형 조회, 특정 코드면 해당 유형만 */
    Page<Post> findPosts(String typeCode, PostSortType sort, String authorMbti, String targetMbti, Pageable pageable);

    /** 어드민 전용 — 숨겨진 게시글 포함, typeCode·isVisible·authorNickname 필터, sortDir 정렬 지원 */
    Page<Post> findAllPostsForAdmin(String typeCode, Boolean isVisible, String authorNickname,
            String sortDir, Pageable pageable);

    /** 내 게시글 목록 — userId 필수, typeCode null 이면 전체 유형 조회 */
    Page<Post> findMyPosts(UUID userId, String typeCode, PostSortType sort, Pageable pageable);

    /** 마이페이지 프리뷰 — 최근 4개, 고정 정렬(최신순) */
    List<Post> findMyPostsPreview(UUID userId);
}
