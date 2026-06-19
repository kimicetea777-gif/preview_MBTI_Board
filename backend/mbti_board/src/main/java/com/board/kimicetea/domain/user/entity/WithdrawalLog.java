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

/** 탈퇴 이벤트 기록 — PII 없이 탈퇴 시각만 보관, 유저 행 삭제 후에도 추이 집계에 활용 */
@Entity
@Table(name = "withdrawal_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp
    @Column(name = "withdrawn_at", nullable = false, updatable = false)
    private Instant withdrawnAt;

    public static WithdrawalLog create() {
        return new WithdrawalLog();
    }
}
