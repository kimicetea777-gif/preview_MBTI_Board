package com.board.kimicetea.global.membershipsync;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.board.kimicetea.global.alert.AlertMarkers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipEventRetryServiceTest {

    private static final String WITHDRAWN = MembershipEventResponse.TYPE_CLIENT_WITHDRAWN;
    private static final String ACTIVATED = MembershipEventResponse.TYPE_CLIENT_ACTIVATED;

    @Mock
    private MembershipEventFailureRepository failureRepository;

    @Mock
    private MembershipEventApplier applier;

    private MembershipEventRetryService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new MembershipEventRetryService(failureRepository, applier);
        logger = (Logger) LoggerFactory.getLogger(MembershipEventRetryService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    private static MembershipEventFailure row(final long eventId, final UUID user, final String type, final int failCount) {
        final MembershipEventFailure row = new MembershipEventFailure();
        ReflectionTestUtils.setField(row, "eventId", eventId);
        ReflectionTestUtils.setField(row, "seq", eventId);
        ReflectionTestUtils.setField(row, "authServerId", user);
        ReflectionTestUtils.setField(row, "eventType", type);
        ReflectionTestUtils.setField(row, "changedAt", Instant.parse("2026-08-01T04:00:00Z"));
        ReflectionTestUtils.setField(row, "failCount", failCount);
        ReflectionTestUtils.setField(row, "nextAttemptAt", Instant.now().minus(Duration.ofSeconds(1)));
        return row;
    }

    private void dueHeads(final MembershipEventFailure... heads) {
        when(failureRepository.findDueHeads(any(), eq(MembershipEventRetryService.BATCH_LIMIT))).thenReturn(List.of(heads));
    }

    @Nested
    @DisplayName("성공")
    class Success {

        @Test
        @DisplayName("행을 지우고 같은 유저의 다음 행을 이어서 처리한다")
        void deletesAndContinuesWithSameUser() {
            final UUID user = UUID.randomUUID();
            final MembershipEventFailure first = row(10L, user, WITHDRAWN, 1);
            final MembershipEventFailure second = row(11L, user, ACTIVATED, 0);
            dueHeads(first);
            when(failureRepository.findFirstByAuthServerIdOrderBySeqAsc(user))
                    .thenReturn(Optional.of(second))
                    .thenReturn(Optional.empty());

            service.retryDue();

            final InOrder order = inOrder(applier, failureRepository);
            order.verify(applier).apply(WITHDRAWN, 10L, user, first.getChangedAt());
            order.verify(failureRepository).deleteResolved(10L);
            order.verify(applier).apply(ACTIVATED, 11L, user, second.getChangedAt());
            order.verify(failureRepository).deleteResolved(11L);
        }

        @Test
        @DisplayName("due 행이 없으면 아무 일도 하지 않는다")
        void noDueRows() {
            dueHeads();

            service.retryDue();

            verifyNoInteractions(applier);
            verify(failureRepository, never()).deleteResolved(any());
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {

        @Test
        @DisplayName("scheduleRetry(eventId, now + retryDelay(failCount + 1), 메시지)를 호출하고 같은 유저의 다음 행은 처리하지 않는다")
        void schedulesRetryAndStopsUser() {
            final UUID user = UUID.randomUUID();
            dueHeads(row(20L, user, WITHDRAWN, 2));
            doThrow(new IllegalStateException("boom")).when(applier).apply(any(), eq(20L), any(), any());
            final Instant before = Instant.now();

            service.retryDue();

            final ArgumentCaptor<Instant> next = ArgumentCaptor.forClass(Instant.class);
            verify(failureRepository).scheduleRetry(eq(20L), next.capture(), eq("IllegalStateException: boom"));
            assertThat(next.getValue()).isAfterOrEqualTo(before.plus(Duration.ofMinutes(1))); // 3번째 실패 → 1분
            verify(failureRepository, never()).deleteResolved(any());
            verify(failureRepository, never()).findFirstByAuthServerIdOrderBySeqAsc(any());
        }

        @Test
        @DisplayName("순서: 앞 행(WITHDRAWN)이 실패하는 동안 뒤 행(ACTIVATED)은 적용되지 않는다")
        void laterRowWaitsWhileHeadFails() {
            final UUID user = UUID.randomUUID();
            dueHeads(row(30L, user, WITHDRAWN, 1));
            doThrow(new IllegalStateException("boom")).when(applier).apply(eq(WITHDRAWN), eq(30L), any(), any());

            service.retryDue();

            verify(applier, never()).apply(eq(ACTIVATED), any(), any(), any());
        }

        @Test
        @DisplayName("데이터 문제로 보이는 실패면 다른 유저의 행은 계속 처리한다")
        void otherUsersContinueOnDataFailure() {
            final UUID failing = UUID.randomUUID();
            final UUID other = UUID.randomUUID();
            dueHeads(row(40L, failing, WITHDRAWN, 1), row(41L, other, WITHDRAWN, 1));
            doThrow(new IllegalStateException("bad data")).when(applier).apply(any(), eq(40L), any(), any());
            when(failureRepository.findFirstByAuthServerIdOrderBySeqAsc(other)).thenReturn(Optional.empty());

            service.retryDue();

            verify(applier).apply(WITHDRAWN, 41L, other, Instant.parse("2026-08-01T04:00:00Z"));
            verify(failureRepository).deleteResolved(41L);
        }

        @Test
        @DisplayName("연결 계열 실패면 실패를 기록한 뒤 이번 틱의 남은 행을 처리하지 않는다")
        void stopsTickOnInfraFailure() {
            dueHeads(row(50L, UUID.randomUUID(), WITHDRAWN, 1), row(51L, UUID.randomUUID(), WITHDRAWN, 1));
            doThrow(new RedisConnectionFailureException("redis down")).when(applier).apply(any(), eq(50L), any(), any());

            service.retryDue();

            verify(failureRepository).scheduleRetry(eq(50L), any(), anyString());
            verify(applier, never()).apply(any(), eq(51L), any(), any());
        }
    }

    @Nested
    @DisplayName("알림")
    class Alerting {

        @ParameterizedTest(name = "이전 실패 {0}회 → 이번이 {1}회째 실패, 알림={2}")
        @CsvSource({"3, 4, false", "4, 5, true", "5, 6, false"})
        @DisplayName("fail_count가 5가 되는 실패에서만 MEMBERSHIP_SYNC_FAIL ERROR를 남기고, 나머지는 WARN")
        void alertsOnlyOnFifthFailure(final int previousFailures, final int attempt, final boolean alerted) {
            dueHeads(row(60L, UUID.randomUUID(), WITHDRAWN, previousFailures));
            doThrow(new IllegalStateException("boom")).when(applier).apply(any(), any(), any(), any());

            service.retryDue();

            final long errors = logs.list.stream()
                    .filter(event -> event.getLevel() == Level.ERROR)
                    .filter(event -> event.getMarkerList() != null
                            && event.getMarkerList().contains(AlertMarkers.MEMBERSHIP_SYNC_FAIL))
                    .count();
            assertThat(errors).isEqualTo(alerted ? 1 : 0);
            assertThat(logs.list).noneSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getMarkerList()).isNullOrEmpty(); // 마커 없는 ERROR 금지
            });
        }
    }

    @Nested
    @DisplayName("한 틱의 상한")
    class BatchLimit {

        @Test
        @DisplayName("같은 유저를 이어서 처리한 행도 BATCH_LIMIT에 포함된다")
        void chainedRowsCountTowardLimit() {
            final UUID user = UUID.randomUUID();
            final List<MembershipEventFailure> chain = new ArrayList<>();
            for (int i = 1; i <= MembershipEventRetryService.BATCH_LIMIT + 10; i++) {
                chain.add(row(1_000L + i, user, ACTIVATED, 0));
            }
            dueHeads(chain.get(0));
            final AtomicInteger next = new AtomicInteger(1);
            lenient().when(failureRepository.findFirstByAuthServerIdOrderBySeqAsc(user)).thenAnswer(invocation -> {
                final int index = next.getAndIncrement();
                return index < chain.size() ? Optional.of(chain.get(index)) : Optional.empty();
            });

            service.retryDue();

            verify(applier, times(MembershipEventRetryService.BATCH_LIMIT)).apply(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("정적 규칙")
    class StaticRules {

        @ParameterizedTest(name = "{0}번째 실패 → {1}초")
        @CsvSource({"1, 10", "2, 30", "3, 60", "4, 300", "5, 900", "6, 1800", "7, 3600", "100, 3600"})
        @DisplayName("retryDelay: 표를 넘으면 1시간")
        void retryDelay(final int failCount, final long seconds) {
            assertThat(MembershipEventRetryService.retryDelay(failCount)).isEqualTo(Duration.ofSeconds(seconds));
        }

        @Test
        @DisplayName("isInfraFailure: DB·Redis 연결 문제만 true(원인 사슬도 본다)")
        void isInfraFailure() {
            assertThat(MembershipEventRetryService.isInfraFailure(new RedisConnectionFailureException("down"))).isTrue();
            assertThat(MembershipEventRetryService.isInfraFailure(new QueryTimeoutException("timeout"))).isTrue();
            assertThat(MembershipEventRetryService.isInfraFailure(new CannotCreateTransactionException("no conn"))).isTrue();
            assertThat(MembershipEventRetryService.isInfraFailure(
                    new RuntimeException("wrapped", new RedisConnectionFailureException("down")))).isTrue();
            assertThat(MembershipEventRetryService.isInfraFailure(new IllegalStateException("bad data"))).isFalse();
        }

        @Test
        @DisplayName("describeError: 예외 종류와 메시지를 500자로 자른다")
        void describeError() {
            assertThat(MembershipEventRetryService.describeError(new IllegalStateException())).isEqualTo("IllegalStateException");
            assertThat(MembershipEventRetryService.describeError(new IllegalStateException("x".repeat(600))))
                    .hasSize(MembershipEventRetryService.LAST_ERROR_MAX_LENGTH);
        }
    }
}
