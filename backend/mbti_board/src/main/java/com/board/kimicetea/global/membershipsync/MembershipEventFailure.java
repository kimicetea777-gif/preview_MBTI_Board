package com.board.kimicetea.global.membershipsync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * [역할] 로컬 반영에 실패했거나, 같은 유저의 앞선 실패 때문에 순서를 지키려고 대기 중인 AS 멤버십 이벤트 한 건.
 *        행이 있다는 것 자체가 "아직 반영되지 않음"이고, 반영에 성공하면 행을 지운다.
 * [주의사항] 등록·갱신·삭제는 MembershipEventFailureRepository의 쿼리로만 한다 — 이 엔티티는 조회 전용이다
 *           (PostImage와 같은 방식). 같은 유저 안의 반영 순서는 eventId가 아니라 seq(줄 선 순서)로 정한다.
 * 설계: Document/membership_event_retry_plan.md
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "membership_event_failures")
public class MembershipEventFailure {

    /** AS user_client_registration_logs.id — 중복 방지용 */
    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "seq", nullable = false, insertable = false, updatable = false)
    private Long seq;

    @Column(name = "auth_server_id", nullable = false)
    private UUID authServerId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    /** AS 쪽 발생 시각 — CLIENT_WITHDRAWN의 유예기간 계산 기준이라 그대로 핸들러에 넘긴다 */
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
