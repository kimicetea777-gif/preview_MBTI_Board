package com.board.kimicetea.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 탈퇴 유저 법적 보존 로그 — 수사 협조를 위해 email(암호화)·phone_hash·IP를 12개월 보관
 * 주의: email_enc는 RetentionEncryptionService(AES)로 암호화됨 — 평문 저장 금지
 */
@Entity
@Table(name = "user_retention_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRetentionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email_enc", nullable = false, columnDefinition = "TEXT")
    private String emailEnc;

    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    @Column(name = "withdrawal_ip", nullable = false, length = 45)
    private String withdrawalIp;

    @CreationTimestamp
    @Column(name = "withdrawn_at", nullable = false, updatable = false)
    private Instant withdrawnAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    /** 탈퇴 시점 정보를 받아 12개월 후 만료되는 보존 로그 생성 */
    public static UserRetentionLog of(final UUID userId, final String emailEnc,
            final String phoneHash, final String withdrawalIp) {
        final UserRetentionLog log = new UserRetentionLog();
        log.userId = userId;
        log.emailEnc = emailEnc;
        log.phoneHash = phoneHash;
        log.withdrawalIp = withdrawalIp;
        log.expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);
        return log;
    }
}
