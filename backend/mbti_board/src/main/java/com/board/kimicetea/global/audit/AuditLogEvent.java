package com.board.kimicetea.global.audit;

import java.util.UUID;

/** 감사 로그 기록 요청 이벤트 — ApplicationEventPublisher를 통해 발행 */
public record AuditLogEvent(AuditEvent event, UUID actorId, String actorRole, String ip) {}
