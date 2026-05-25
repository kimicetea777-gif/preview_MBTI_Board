package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.entity.VotePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 투표 게시글 메타 레포지토리
 * 주의: options는 JOIN FETCH 쿼리로만 로드 — LazyInitializationException 방지
 */
public interface VotePostRepository extends JpaRepository<VotePost, Long> {

    @Query("SELECT v FROM VotePost v JOIN FETCH v.options WHERE v.id = :id")
    Optional<VotePost> findWithOptionsById(@Param("id") Long id);
}
