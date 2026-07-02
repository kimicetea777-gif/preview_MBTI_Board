package com.board.kimicetea.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 감사 로그 기록 진입점 — 이벤트 발행만 담당, 실제 저장은 AuditLogEventHandler가 처리
 * 주의: 호출자 트랜잭션 커밋 이후에 저장되므로 롤백 시 로그가 남지 않음
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ApplicationEventPublisher eventPublisher;

    public void record(final AuditEvent event, final UUID actorId,
                       final String actorRole, final String ip) {
        eventPublisher.publishEvent(new AuditLogEvent(event, actorId, actorRole, ip));
    }
}
