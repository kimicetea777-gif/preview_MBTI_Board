package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 기본 CRUD + viewCount 원자적 증가
 * 주의: viewCount는 이 메서드로만 증가 — 엔티티 직접 수정 금지
 */
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    boolean existsByIdAndIsVisibleTrue(Long id);

    @Query("SELECT p FROM Post p JOIN FETCH p.postType JOIN FETCH p.authorMbti LEFT JOIN FETCH p.user LEFT JOIN FETCH p.targetMbti WHERE p.id = :id AND p.isVisible = true")
    Optional<Post> findWithDetailsById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void increaseViewCount(@Param("id") Long id);
}
