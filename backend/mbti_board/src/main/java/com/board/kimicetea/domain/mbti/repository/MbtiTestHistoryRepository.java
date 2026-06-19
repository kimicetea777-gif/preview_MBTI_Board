package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.MbtiTestHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MbtiTestHistoryRepository extends JpaRepository<MbtiTestHistory, Long> {

    @Query("SELECT h FROM MbtiTestHistory h JOIN FETCH h.mbti WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<MbtiTestHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT h FROM MbtiTestHistory h JOIN FETCH h.mbti WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<MbtiTestHistory> findLatestByUserId(@Param("userId") UUID userId, Pageable pageable);

    // ── 어드민 통계 쿼리 ─────────────────────────────────────────────────────────────

    /** 검사 결과 MBTI 타입별 분포 */
    @Query(value = "SELECT mbti_id AS name, COUNT(*) AS value FROM mbti_test_history GROUP BY mbti_id ORDER BY value DESC", nativeQuery = true)
    List<Object[]> countGroupByResultRaw();

    /** EI·SN·TF·JP 차원별 평균 점수 — 단일 행 [avgEi, avgSn, avgTf, avgJp] */
    @Query(value = "SELECT AVG(ei_score), AVG(sn_score), AVG(tf_score), AVG(jp_score) FROM mbti_test_history", nativeQuery = true)
    List<Object[]> avgScores();

    /** 최근 :since 이후 일별 검사 수 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD') AS date,
                   COUNT(*)                                         AS value
            FROM mbti_test_history
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC('day', created_at)
            ORDER BY DATE_TRUNC('day', created_at)
            """, nativeQuery = true)
    List<Object[]> findTestTrendRaw(@Param("since") Instant since);

    /** :from 이상 :to 미만 구간의 검사 수 */
    @Query("SELECT COUNT(h) FROM MbtiTestHistory h WHERE h.createdAt >= :from AND h.createdAt < :to")
    long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** 유저별 검사 횟수 버킷 — 단일 행 [once, twice, threeOrMore] */
    @Query(value = """
            SELECT
              SUM(CASE WHEN test_count = 1 THEN 1 ELSE 0 END),
              SUM(CASE WHEN test_count = 2 THEN 1 ELSE 0 END),
              SUM(CASE WHEN test_count >= 3 THEN 1 ELSE 0 END)
            FROM (SELECT user_id, COUNT(*) AS test_count FROM mbti_test_history GROUP BY user_id) sub
            """, nativeQuery = true)
    List<Object[]> countRetakeBuckets();
}
