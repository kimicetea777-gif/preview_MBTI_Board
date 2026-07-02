package com.board.kimicetea.domain.notice.repository;

import com.board.kimicetea.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 공지사항 레포지토리
 * 주의: findActive는 is_active=true + 기간 조건(starts_at/ends_at NULL 허용)을 함께 검사
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 현재 시각 기준 활성 공지 조회 — priority DESC, id DESC 정렬
     * starts_at NULL → 즉시 노출 / ends_at NULL → 무기한 노출
     */
    @Query("""
            SELECT n FROM Notice n
            WHERE n.isActive = true
              AND (n.startsAt IS NULL OR n.startsAt <= :now)
              AND (n.endsAt   IS NULL OR n.endsAt   >  :now)
            ORDER BY n.priority DESC, n.id DESC
            """)
    List<Notice> findActive(@Param("now") Instant now);
}
