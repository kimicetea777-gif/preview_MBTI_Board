package com.board.kimicetea.global.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 감사 로그 비동기 저장 핸들러
 * 주의: @TransactionalEventListener(AFTER_COMMIT)로 호출자 트랜잭션 커밋 이후에만 저장
 * — 호출자 롤백 시 로그가 남는 문제 방지. fallbackExecution=true로 비트랜잭션 호출도 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogEventHandler {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(final AuditLogEvent auditLogEvent) {
        try {
            auditLogRepository.save(
                    AuditLog.of(auditLogEvent.event(), auditLogEvent.actorId(), auditLogEvent.actorRole(),
                            auditLogEvent.ip()));
        } catch (Exception e) {
            log.error("감사 로그 저장 실패 [{}] actorId={}: {}",
                    auditLogEvent.event(), auditLogEvent.actorId(), e.getMessage(), e);
        }
    }
}
