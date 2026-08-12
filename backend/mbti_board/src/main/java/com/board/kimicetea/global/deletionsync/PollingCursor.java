package com.board.kimicetea.global.deletionsync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * AS 내부 폴링 API를 커서 기반으로 순회하기 위한 진행 상태. {@code pollerName}당 row 하나
 * (지금은 {@link DeletedUserSyncService#POLLER_NAME}뿐).
 */
@Entity
@Getter
@Table(name = "polling_cursors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollingCursor {

    @Id
    @Column(name = "poller_name")
    private String pollerName;

    @Column(name = "since_timestamp", nullable = false)
    private Instant sinceTimestamp;

    @Column(name = "since_id")
    private Long sinceId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 최초 폴링 — memo.md/connect_manual.md 규칙대로 since=EPOCH, sinceId 없음 */
    public static PollingCursor initial(final String pollerName) {
        final PollingCursor cursor = new PollingCursor();
        cursor.pollerName = pollerName;
        cursor.sinceTimestamp = Instant.EPOCH;
        return cursor;
    }

    /** 로컬 처리가 성공한 항목의 (deletedAt, id)로만 전진시킨다 — 실패한 항목 이전에서 멈춰야 함 */
    public void advance(final Instant sinceTimestamp, final Long sinceId) {
        this.sinceTimestamp = sinceTimestamp;
        this.sinceId = sinceId;
    }
}
