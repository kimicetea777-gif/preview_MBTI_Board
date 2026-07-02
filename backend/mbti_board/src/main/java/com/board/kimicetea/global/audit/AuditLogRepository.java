package com.board.kimicetea.global.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 감사 로그 저장소 — 조회보다 INSERT 위주로 사용
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
