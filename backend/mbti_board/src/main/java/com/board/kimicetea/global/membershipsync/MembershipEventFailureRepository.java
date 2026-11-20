package com.board.kimicetea.global.membershipsync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * [역할] membership_event_failures 접근 — 줄 세우기, 재시도 대상 조회, 실패 기록, 성공 시 삭제, 존재 확인.
 * [주의사항] @Modifying 쿼리는 기본 트랜잭션이 없어 메서드에 @Transactional을 붙인다. 폴러·재시도 서비스는
 *           트랜잭션 밖에서 부르므로 문장 단위로 짧게 커밋된다(PostImageRepository와 같은 방식).
 *           같은 유저 안의 순서는 seq로만 판단한다 — event_id 순서는 AS의 발생 순서와 같다는 보장이 없다.
 */
public interface MembershipEventFailureRepository extends JpaRepository<MembershipEventFailure, Long> {

    /**
     * 이벤트를 줄 세운다. 같은 이벤트를 다시 받으면(폴러가 커서 저장 전에 죽은 경우) 무시하고 처음 순번을 유지한다.
     * 반환값 1 = 이번에 줄 섬, 0 = 이미 줄 서 있음
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO membership_event_failures
                (event_id, auth_server_id, event_type, changed_at, fail_count, next_attempt_at, last_error)
            VALUES (:eventId, :authServerId, :eventType, :changedAt, :failCount, :nextAttemptAt, :lastError)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int enqueue(@Param("eventId") Long eventId, @Param("authServerId") UUID authServerId,
            @Param("eventType") String eventType, @Param("changedAt") Instant changedAt,
            @Param("failCount") int failCount, @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError);

    /**
     * 유저별 맨 앞 행(그 유저의 최소 seq)이면서 시도할 차례가 된 행. 뒤에 줄 선 행은 앞 행이 지워지기 전까지
     * 나오지 않아 유저 단위 순서가 지켜진다.
     */
    @Query(value = """
            SELECT f.* FROM membership_event_failures f
            WHERE f.next_attempt_at <= :now
              AND f.seq = (SELECT MIN(x.seq) FROM membership_event_failures x
                           WHERE x.auth_server_id = f.auth_server_id)
            ORDER BY f.next_attempt_at
            LIMIT :limit
            """, nativeQuery = true)
    List<MembershipEventFailure> findDueHeads(@Param("now") Instant now, @Param("limit") int limit);

    /** 같은 유저의 (지금) 맨 앞 행 — 앞 행을 지운 뒤 이어서 처리할 때 쓴다 */
    Optional<MembershipEventFailure> findFirstByAuthServerIdOrderBySeqAsc(UUID authServerId);

    /** 확정 배치가 "이 유저에게 미해결 이벤트가 있는가"를 볼 때 쓴다 */
    boolean existsByAuthServerId(UUID authServerId);

    /** 폴러가 페이지마다 한 번 읽는, 미해결 이벤트가 있는 유저 집합 */
    @Query("SELECT DISTINCT f.authServerId FROM MembershipEventFailure f")
    Set<UUID> findAllAuthServerIds();

    /** 실패 횟수를 1 올리고 다음 시도 시각을 미룬다 */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE membership_event_failures
            SET fail_count = fail_count + 1, next_attempt_at = :nextAttemptAt,
                last_error = :lastError, updated_at = NOW()
            WHERE event_id = :eventId
            """, nativeQuery = true)
    int scheduleRetry(@Param("eventId") Long eventId, @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError);

    /** 반영에 성공한 행 삭제 — 엔티티를 읽어 오지 않고 바로 지운다 */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM membership_event_failures WHERE event_id = :eventId", nativeQuery = true)
    int deleteResolved(@Param("eventId") Long eventId);

    /** 04:15 장기 방치 점검 — 기준 시각보다 먼저 줄 선 행 수 */
    long countByCreatedAtBefore(Instant cutoff);
}
