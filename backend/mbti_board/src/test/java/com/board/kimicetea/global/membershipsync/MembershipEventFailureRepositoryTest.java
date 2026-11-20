package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * membership_event_failures 쿼리를 실제 PostgreSQL에서 확인한다(V15 마이그레이션 포함).
 * 주의: 실제 DB가 필요하다 — application-test.yaml의 datasource(기본 localhost:5432). 각 테스트는 롤백된다.
 *       로컬 DB에 다른 행이 남아 있어도 깨지지 않도록 결과는 이 테스트가 만든 유저·이벤트로 걸러서 본다.
 * 상태 확인은 영속성 컨텍스트 캐시를 피하려고 JdbcTemplate으로 직접 읽는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class MembershipEventFailureRepositoryTest {

    /** 실제 AS 로그 id와 겹치지 않을 만큼 큰 값에서 시작 */
    private static final AtomicLong EVENT_IDS = new AtomicLong(9_000_000_000L + System.nanoTime() % 1_000_000_000L);
    private static final String WITHDRAWN = MembershipEventResponse.TYPE_CLIENT_WITHDRAWN;
    private static final String ACTIVATED = MembershipEventResponse.TYPE_CLIENT_ACTIVATED;

    @Autowired private MembershipEventFailureRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("enqueue: 같은 event_id를 두 번 넣으면 두 번째는 0을 반환하고 행은 하나다")
    void enqueueIsIdempotent() {
        final long eventId = nextEventId();
        final UUID user = UUID.randomUUID();

        assertThat(enqueue(eventId, user, WITHDRAWN, 1, Instant.now())).isEqualTo(1);
        assertThat(enqueue(eventId, user, WITHDRAWN, 1, Instant.now())).isZero();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM membership_event_failures WHERE event_id = ?", Long.class, eventId))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("findDueHeads: 한 유저의 행이 모두 due여도 맨 앞(최소 seq) 행만 나온다 — event_id가 더 커도 먼저 줄 선 행이 맨 앞")
    void findDueHeadsReturnsOnlyHeadBySeq() {
        final UUID user = UUID.randomUUID();
        final long first = nextEventId() + 100;   // 먼저 줄 서지만 event_id는 더 크다
        final long second = nextEventId();
        enqueue(first, user, WITHDRAWN, 1, past());
        enqueue(second, user, ACTIVATED, 0, past());

        final List<MembershipEventFailure> heads = headsOf(user);

        assertThat(heads).singleElement().satisfies(row -> {
            assertThat(row.getEventId()).isEqualTo(first);
            assertThat(row.getEventType()).isEqualTo(WITHDRAWN);
        });
    }

    @Test
    @DisplayName("findDueHeads: 맨 앞 행이 아직 due가 아니면 뒤 행이 due여도 그 유저는 나오지 않는다. 다른 유저는 나온다")
    void findDueHeadsSkipsUserWhoseHeadIsNotDue() {
        final UUID blocked = UUID.randomUUID();
        final UUID other = UUID.randomUUID();
        enqueue(nextEventId(), blocked, WITHDRAWN, 3, Instant.now().plus(Duration.ofHours(1)));
        enqueue(nextEventId(), blocked, ACTIVATED, 0, past());
        final long otherEvent = nextEventId();
        enqueue(otherEvent, other, WITHDRAWN, 1, past());

        assertThat(headsOf(blocked)).isEmpty();
        assertThat(headsOf(other)).extracting(MembershipEventFailure::getEventId).containsExactly(otherEvent);
    }

    @Test
    @DisplayName("findDueHeads: limit이 적용된다")
    void findDueHeadsAppliesLimit() {
        for (int i = 0; i < 3; i++) {
            enqueue(nextEventId(), UUID.randomUUID(), WITHDRAWN, 1, past());
        }

        assertThat(repository.findDueHeads(Instant.now(), 2)).hasSize(2);
    }

    @Test
    @DisplayName("findFirstByAuthServerIdOrderBySeqAsc: 줄 선 순서상 맨 앞 행을 돌려준다")
    void findFirstBySeq() {
        final UUID user = UUID.randomUUID();
        final long first = nextEventId() + 100;
        enqueue(first, user, WITHDRAWN, 0, past());
        enqueue(nextEventId(), user, ACTIVATED, 0, past());

        assertThat(repository.findFirstByAuthServerIdOrderBySeqAsc(user))
                .get().extracting(MembershipEventFailure::getEventId).isEqualTo(first);
    }

    @Test
    @DisplayName("existsByAuthServerId, findAllAuthServerIds: 미해결 행이 있는 유저만 나온다")
    void existsAndDistinctUsers() {
        final UUID user = UUID.randomUUID();
        final UUID clean = UUID.randomUUID();
        enqueue(nextEventId(), user, WITHDRAWN, 1, past());
        enqueue(nextEventId(), user, ACTIVATED, 0, past());

        assertThat(repository.existsByAuthServerId(user)).isTrue();
        assertThat(repository.existsByAuthServerId(clean)).isFalse();
        assertThat(repository.findAllAuthServerIds()).contains(user).doesNotContain(clean);
    }

    @Test
    @DisplayName("scheduleRetry: fail_count를 1 올리고 next_attempt_at, last_error를 갱신한다")
    void scheduleRetryUpdatesRow() {
        final long eventId = nextEventId();
        enqueue(eventId, UUID.randomUUID(), WITHDRAWN, 2, past());
        final Instant next = Instant.now().plus(Duration.ofMinutes(5));

        assertThat(repository.scheduleRetry(eventId, next, "redis down")).isEqualTo(1);

        final Map<String, Object> row = jdbc.queryForMap(
                "SELECT fail_count, next_attempt_at, last_error FROM membership_event_failures WHERE event_id = ?", eventId);
        assertThat(row.get("fail_count")).isEqualTo(3);
        assertThat(((Timestamp) row.get("next_attempt_at")).toInstant())
                .isCloseTo(next, within(1, ChronoUnit.MILLIS));
        assertThat(row.get("last_error")).isEqualTo("redis down");
    }

    @Test
    @DisplayName("deleteResolved: 그 행만 지운다")
    void deleteResolvedRemovesOnlyThatRow() {
        final UUID user = UUID.randomUUID();
        final long resolved = nextEventId();
        final long remaining = nextEventId();
        enqueue(resolved, user, WITHDRAWN, 1, past());
        enqueue(remaining, user, ACTIVATED, 0, past());

        assertThat(repository.deleteResolved(resolved)).isEqualTo(1);

        assertThat(repository.findFirstByAuthServerIdOrderBySeqAsc(user))
                .get().extracting(MembershipEventFailure::getEventId).isEqualTo(remaining);
    }

    @Test
    @DisplayName("countByCreatedAtBefore: 기준 시각보다 먼저 줄 선 행만 센다")
    void createdAtBefore() {
        final long old = nextEventId();
        final long fresh = nextEventId();
        enqueue(old, UUID.randomUUID(), WITHDRAWN, 5, past());
        enqueue(fresh, UUID.randomUUID(), WITHDRAWN, 1, past());
        final Instant twoDaysAgo = Instant.now().minus(Duration.ofDays(2));
        jdbc.update("UPDATE membership_event_failures SET created_at = ? WHERE event_id = ?",
                Timestamp.from(twoDaysAgo), old);
        final Instant cutoff = Instant.now().minus(Duration.ofDays(1));

        assertThat(repository.countByCreatedAtBefore(cutoff)).isGreaterThanOrEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM membership_event_failures WHERE event_id IN (?, ?) AND created_at < ?",
                Long.class, old, fresh, Timestamp.from(cutoff)))
                .isEqualTo(1L);
    }

    private int enqueue(final long eventId, final UUID user, final String type, final int failCount,
            final Instant nextAttemptAt) {
        return repository.enqueue(eventId, user, type, Instant.now(), failCount, nextAttemptAt, null);
    }

    private List<MembershipEventFailure> headsOf(final UUID user) {
        return repository.findDueHeads(Instant.now(), 1_000).stream()
                .filter(row -> row.getAuthServerId().equals(user))
                .toList();
    }

    private static Instant past() {
        return Instant.now().minus(Duration.ofMinutes(1));
    }

    private static long nextEventId() {
        return EVENT_IDS.getAndAdd(1_000);
    }
}
