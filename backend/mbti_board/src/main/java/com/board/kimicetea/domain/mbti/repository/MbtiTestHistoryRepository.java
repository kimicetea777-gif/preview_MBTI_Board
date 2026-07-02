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
        List<MbtiTestHistory> findLatestByUserId(@Param("userId") UUID userId, Pageable pageable);

        // ── 어드민 통계 쿼리 ─────────────────────────────────────────────────────────────

        interface NameValueRow { String getName(); Long getValue(); }
        interface DateValueRow { String getDate(); Long getValue(); }
        interface AvgScoreRow { Double getAvgEi(); Double getAvgSn(); Double getAvgTf(); Double getAvgJp(); }
        interface RetakeBucketRow { Long getOnce(); Long getTwice(); Long getThreeOrMore(); }

        /** 검사 결과 MBTI 타입별 분포 */
        @Query(value = "SELECT mbti_id AS name, COUNT(*) AS value FROM mbti_test_history GROUP BY mbti_id ORDER BY value DESC", nativeQuery = true)
        List<NameValueRow> countGroupByResultRaw();

        /** EI·SN·TF·JP 차원별 평균 점수 */
        @Query(value = "SELECT AVG(ei_score) AS avgEi, AVG(sn_score) AS avgSn, AVG(tf_score) AS avgTf, AVG(jp_score) AS avgJp FROM mbti_test_history", nativeQuery = true)
        List<AvgScoreRow> avgScores();

        /** 최근 :since 이후 일별 검사 수 */
        @Query(value = """
                        SELECT TO_CHAR(DATE_TRUNC('day', created_at), 'MM-DD') AS date,
                               COUNT(*)                                         AS value
                        FROM mbti_test_history
                        WHERE created_at >= :since
                        GROUP BY DATE_TRUNC('day', created_at)
                        ORDER BY DATE_TRUNC('day', created_at)
                        """, nativeQuery = true)
        List<DateValueRow> findTestTrendRaw(@Param("since") Instant since);

        /** :from 이상 :to 미만 구간의 검사 수 */
        @Query("SELECT COUNT(h) FROM MbtiTestHistory h WHERE h.createdAt >= :from AND h.createdAt < :to")
        long countByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

        /** 유저별 검사 횟수 버킷 */
        @Query(value = """
                        SELECT
                          SUM(CASE WHEN test_count = 1 THEN 1 ELSE 0 END)  AS once,
                          SUM(CASE WHEN test_count = 2 THEN 1 ELSE 0 END)  AS twice,
                          SUM(CASE WHEN test_count >= 3 THEN 1 ELSE 0 END) AS threeOrMore
                        FROM (SELECT user_id, COUNT(*) AS test_count FROM mbti_test_history GROUP BY user_id) sub
                        """, nativeQuery = true)
        List<RetakeBucketRow> countRetakeBuckets();
}
