package com.board.kimicetea.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * 개인정보보호법 시행령 제30조 — 접속 기록 보관용 감사 로그
 * 주의: payload에 이메일·전화번호 원문 등 PII 유입 금지
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "actor_role", nullable = false, updatable = false, length = 20)
    private String actorRole;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, updatable = false, length = 50)
    private AuditEvent event;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", updatable = false)
    private String payload;

    @Column(name = "ip", nullable = false, updatable = false, length = 45)
    private String ip;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*-----------------------------------------------------------------------*/
    public static @NonNull AuditLog of(final AuditEvent event, final UUID actorId,
            final String actorRole, final String ip) {
        final AuditLog log = new AuditLog();
        log.event = event;
        log.actorId = actorId;
        log.actorRole = actorRole;
        log.ip = ip;
        return log;
    }

    /** 관리자가 특정 유저를 대상으로 한 이벤트 기록용 */
    public static @NonNull AuditLog ofWithTarget(final AuditEvent event, final UUID actorId,
            final String actorRole, final UUID targetId,
            final String ip) {
        final AuditLog log = of(event, actorId, actorRole, ip);
        log.targetId = targetId;
        return log;
    }
}
