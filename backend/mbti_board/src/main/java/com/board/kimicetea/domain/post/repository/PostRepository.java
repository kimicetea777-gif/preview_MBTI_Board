package com.board.kimicetea.domain.post.repository;

import com.board.kimicetea.domain.post.dto.PostSitemapResponse;
import com.board.kimicetea.domain.post.entity.Post;
import com.board.kimicetea.domain.post.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게시글 기본 CRUD + viewCount 원자적 증가
 * 주의: viewCount는 이 메서드로만 증가 — 엔티티 직접 수정 금지
 */
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    boolean existsByIdAndIsVisibleTrue(Long id);

    @Query("SELECT p FROM Post p JOIN FETCH p.authorMbti LEFT JOIN FETCH p.user LEFT JOIN FETCH p.targetMbti WHERE p.id = :id AND p.isVisible = true")
    Optional<Post> findWithDetailsById(@Param("id") Long id);

    /** 어드민 전용 — 숨김(isVisible=false) 게시글도 조회 가능 */
    @Query("SELECT p FROM Post p JOIN FETCH p.authorMbti LEFT JOIN FETCH p.user LEFT JOIN FETCH p.targetMbti WHERE p.id = :id")
    Optional<Post> findWithDetailsByIdForAdmin(@Param("id") Long id);

    /** SEO/OG 메타데이터 전용 — title·contentPreview·imageUrls는 스칼라 컬럼이라 JOIN 불필요 */
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.isVisible = true")
    Optional<Post> findVisibleById(@Param("id") Long id);

    /**
     * sitemap.xml 생성 전용 — id·createdAt만 조회, authorMbti/user/votePost/debatePost JOIN 전부 제외
     * 주의: ORDER BY 없이 OFFSET/LIMIT만 쓰면 페이지 간 행 순서가 보장되지 않아 누락·중복이 생길 수 있음 — id 기준 정렬 필수
     */
    @Query(value = "SELECT new com.board.kimicetea.domain.post.dto.PostSitemapResponse(p.id, p.createdAt) "
            + "FROM Post p WHERE p.isVisible = true AND p.postType = :postType ORDER BY p.id ASC",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.isVisible = true AND p.postType = :postType")
    Page<PostSitemapResponse> findSitemapEntries(@Param("postType") PostType postType, Pageable pageable);

    /** 댓글 목록 조회 전용 — PostType 스칼라만 조회해 불필요한 JOIN 제거 */
    @Query("SELECT p.postType FROM Post p WHERE p.id = :id AND p.isVisible = true")
    Optional<PostType> findPostTypeById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void increaseViewCount(@Param("id") Long id);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.isVisible = true")
    long countByUserId(@Param("userId") UUID userId);

    // ── 어드민 통계 쿼리 ─────────────────────────────────────────────────────────────

    /** 숨겨진 게시글 수 */
    @Query(value = "SELECT COUNT(*) FROM posts WHERE is_visible = false", nativeQuery = true)
    long countHidden();

    /** 타입 코드별 게시글 수 */
    @Query(value = "SELECT post_type AS name, COUNT(*) AS value FROM posts GROUP BY post_type ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByTypeRaw();

    /** MBTI별 게시글 수 */
    @Query(value = "SELECT author_mbti AS name, COUNT(*) AS value FROM posts GROUP BY author_mbti ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByMbtiRaw();

    /** 최근 :since 이후 일별 게시글 수 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD') AS date,
                   COUNT(*)                                         AS value
            FROM posts
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('day', created_at)
            ORDER BY DATE_TRUNC('day', created_at)
            """, nativeQuery = true)
    List<Object[]> findPostTrendRaw(@Param("since") Instant since);

    /** :from 이상 :to 미만 구간의 신규 게시글 수 */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :from AND p.createdAt < :to")
    long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);
}
