package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.WithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WithdrawalLogRepository extends JpaRepository<WithdrawalLog, Long> {

    /** 최근 :since 이후 일별 탈퇴 수 — MM-DD 형식 날짜 반환 */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', withdrawn_at), 'MM-DD') AS date,
                   COUNT(*)                                           AS value
            FROM withdrawal_log
            WHERE withdrawn_at >= :since
            GROUP BY DATE_TRUNC('day', withdrawn_at)
            ORDER BY DATE_TRUNC('day', withdrawn_at)
            """, nativeQuery = true)
    List<Object[]> findTrendRaw(@Param("since") Instant since);
}
