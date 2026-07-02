package com.board.kimicetea.domain.comment.repository;

import com.board.kimicetea.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 댓글 기본 CRUD
 * 주의: likeCount·dislikeCount는 DB 트리거가 관리 — JPA 직접 수정 금지
 */
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {

    /** 단건 조회 — 소프트 삭제된 댓글은 제외 (parentId 검증 전용) */
    Optional<Comment> findByIdAndIsVisibleTrue(Long id);

    /**
     * 댓글 단건 조회 + 게시글 소속 검증을 단일 쿼리로 처리
     * 주의: 수정·삭제 전 호출 — 다른 게시글 소속 댓글 접근 시 COMMENT_NOT_FOUND 처리
     */
    Optional<Comment> findByIdAndPost_IdAndIsVisibleTrue(Long id, Long postId);

    /**
     * 특정 게시글의 visible 댓글 전체 조회 (루트 + 대댓글 모두 포함)
     * 주의: 서비스 레이어에서 parent 유무로 루트/대댓글을 분리해야 함
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.authorMbti
            LEFT JOIN FETCH c.user
            LEFT JOIN FETCH c.parent
            WHERE c.post.id = :postId AND c.isVisible = true
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findAllVisibleByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.isVisible = true")
    long countByUserId(@Param("userId") UUID userId);

    // ── 어드민 통계 쿼리 ─────────────────────────────────────────────────────────────

    /** 숨겨진 댓글 수 */
    @Query(value = "SELECT COUNT(*) FROM comments WHERE is_visible = false", nativeQuery = true)
    long countHidden();

    /** MBTI별 댓글 수 */
    @Query(value = "SELECT author_mbti AS name, COUNT(*) AS value FROM comments GROUP BY author_mbti ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByMbtiRaw();

    /** 최근 :since 이후 일별 댓글 수 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD') AS date,
                   COUNT(*)                                         AS value
            FROM comments
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('day', created_at)
            ORDER BY DATE_TRUNC('day', created_at)
            """, nativeQuery = true)
    List<Object[]> findCommentTrendRaw(@Param("since") Instant since);

    /** :from 이상 :to 미만 구간의 신규 댓글 수 */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :from AND c.createdAt < :to")
    long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);
}
