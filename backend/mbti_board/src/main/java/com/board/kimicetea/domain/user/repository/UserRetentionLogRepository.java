package com.board.kimicetea.domain.user.repository;

import com.board.kimicetea.domain.user.entity.UserRetentionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface UserRetentionLogRepository extends JpaRepository<UserRetentionLog, Long> {

    /** 만료된 보존 로그 건수 조회 — 삭제 전 로깅용 */
    long countByExpiresAtBefore(Instant now);

    /** 만료된 보존 로그 bulk DELETE — SELECT 없이 단일 쿼리로 처리 */
    @Modifying
    @Query("DELETE FROM UserRetentionLog u WHERE u.expiresAt < :now")
    void bulkDeleteExpiredBefore(@Param("now") Instant now);
}
