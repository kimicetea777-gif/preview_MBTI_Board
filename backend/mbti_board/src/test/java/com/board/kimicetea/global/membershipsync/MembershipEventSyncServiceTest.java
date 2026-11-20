package com.board.kimicetea.global.membershipsync;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.deletionsync.PollingCursor;
import com.board.kimicetea.global.deletionsync.PollingCursorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipEventSyncServiceTest {

    private static final String WITHDRAWN = MembershipEventResponse.TYPE_CLIENT_WITHDRAWN;
    private static final String ACTIVATED = MembershipEventResponse.TYPE_CLIENT_ACTIVATED;

    @Mock
    private MembershipEventPollClient pollClient;

    @Mock
    private PollingCursorRepository cursorRepository;

    @Mock
    private MembershipEventApplier applier;

    @Mock
    private MembershipEventFailureRepository failureRepository;

    private MembershipEventSyncService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new MembershipEventSyncService(pollClient, cursorRepository, applier, failureRepository,
                new MembershipSyncLock());
        // 일부 테스트(폴링 자체 실패 등)는 save()·조회를 아예 안 부르므로 lenient — 스텁 미사용 검증 예외 방지
        lenient().when(cursorRepository.save(any(PollingCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
        lenient().when(failureRepository.findAllAuthServerIds()).thenReturn(Set.of());

        logger = (Logger) LoggerFactory.getLogger(MembershipEventSyncService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    private MembershipEventResponse eventOf(final String type, final Long logId, final UUID userId, final Instant changedAt) {
        return new MembershipEventResponse(type, logId, userId, "TypeButter", changedAt);
    }

    private void pollReturns(final MembershipEventResponse... events) {
        when(pollClient.poll(any(), any(), eq(500))).thenReturn(new MembershipEventsPollResponse(List.of(events), false));
    }

    private PollingCursor lastSavedCursor(final int expectedSaves) {
        final ArgumentCaptor<PollingCursor> captor = ArgumentCaptor.forClass(PollingCursor.class);
        verify(cursorRepository, times(expectedSaves)).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("커서가 없을 때(최초 폴링)")
    class NoCursorYet {

        @Test
        @DisplayName("EPOCH/sinceId 없이 폴링을 호출한다")
        void pollsFromEpoch() {
            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME)).thenReturn(Optional.empty());
            when(pollClient.poll(Instant.EPOCH, null, 500)).thenReturn(new MembershipEventsPollResponse(List.of(), false));

            assertThat(service.sync()).isTrue();

            verify(pollClient).poll(Instant.EPOCH, null, 500);
        }
    }

    @Nested
    @DisplayName("정상 적용")
    class Apply {

        @Test
        @DisplayName("이벤트를 Applier에 그대로 넘기고 커서를 그 이벤트로 전진시킨다")
        void appliesAndAdvancesCursor() {
            final UUID user = UUID.randomUUID();
            final Instant changedAt = Instant.parse("2026-08-01T04:00:03.120Z");
            pollReturns(eventOf(WITHDRAWN, 101L, user, changedAt));

            assertThat(service.sync()).isTrue();

            verify(applier).apply(WITHDRAWN, 101L, user, changedAt);
            final PollingCursor cursor = lastSavedCursor(1);
            assertThat(cursor.getSinceTimestamp()).isEqualTo(changedAt);
            assertThat(cursor.getSinceId()).isEqualTo(101L);
            verify(failureRepository, never()).enqueue(any(), any(), any(), any(), anyInt(), any(), any());
        }
    }

    @Nested
    @DisplayName("적용에 실패하면")
    class ApplyFailure {

        @Test
        @DisplayName("fail_count=1로 줄 세우고 커서는 그 이벤트까지 전진하며, 다음 이벤트를 계속 처리한다")
        void queuesFailedEventAndContinues() {
            final UUID failing = UUID.randomUUID();
            final UUID other = UUID.randomUUID();
            final MembershipEventResponse failed = eventOf(WITHDRAWN, 201L, failing, Instant.parse("2026-08-01T04:00:00Z"));
            final MembershipEventResponse next = eventOf(WITHDRAWN, 202L, other, Instant.parse("2026-08-01T05:00:00Z"));
            pollReturns(failed, next);
            doThrow(new RuntimeException("DB 순간 장애")).when(applier).apply(eq(WITHDRAWN), eq(201L), eq(failing), any());
            final Instant before = Instant.now();

            assertThat(service.sync()).isTrue();

            final ArgumentCaptor<Instant> nextAttempt = ArgumentCaptor.forClass(Instant.class);
            final ArgumentCaptor<String> lastError = ArgumentCaptor.forClass(String.class);
            verify(failureRepository).enqueue(eq(201L), eq(failing), eq(WITHDRAWN), eq(failed.changedAt()),
                    eq(1), nextAttempt.capture(), lastError.capture());
            assertThat(nextAttempt.getValue()).isAfterOrEqualTo(before.plus(Duration.ofSeconds(10)));
            assertThat(lastError.getValue()).contains("DB 순간 장애");
            verify(applier).apply(WITHDRAWN, 202L, other, next.changedAt()); // 다른 유저는 막히지 않는다
            assertThat(lastSavedCursor(2).getSinceId()).isEqualTo(202L);
        }

        @Test
        @DisplayName("실패한 유저의 같은 페이지 안 후속 이벤트는 적용하지 않고 fail_count=0으로 줄 세운다")
        void queuesLaterEventsOfFailedUser() {
            final UUID user = UUID.randomUUID();
            final MembershipEventResponse withdrawn = eventOf(WITHDRAWN, 301L, user, Instant.parse("2026-08-01T04:00:00Z"));
            final MembershipEventResponse activated = eventOf(ACTIVATED, 302L, user, Instant.parse("2026-08-01T04:30:00Z"));
            pollReturns(withdrawn, activated);
            doThrow(new RuntimeException("Redis 장애")).when(applier).apply(eq(WITHDRAWN), eq(301L), eq(user), any());

            service.sync();

            verify(applier, never()).apply(eq(ACTIVATED), any(), any(), any());
            verify(failureRepository).enqueue(eq(302L), eq(user), eq(ACTIVATED), eq(activated.changedAt()),
                    eq(0), any(), isNull());
            assertThat(lastSavedCursor(2).getSinceId()).isEqualTo(302L);
        }

        @Test
        @DisplayName("적용 실패 로그는 WARN이고 마커 없는 ERROR를 남기지 않는다(UNHANDLED_ERROR 음소거 방지)")
        void logsWarnNotUnmarkedError() {
            final UUID user = UUID.randomUUID();
            pollReturns(eventOf(WITHDRAWN, 401L, user, Instant.now()));
            doThrow(new RuntimeException("DB 순간 장애")).when(applier).apply(any(), any(), any(), any());

            service.sync();

            assertThat(logs.list).anySatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
            assertThat(logs.list).noneSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.ERROR));
        }
    }

    @Nested
    @DisplayName("페이지 시작 시점에 이미 미해결 행이 있는 유저")
    class AlreadyBlockedUser {

        @Test
        @DisplayName("이벤트를 적용하지 않고 줄 세운다. 다른 유저는 정상 적용한다")
        void queuesWithoutApplying() {
            final UUID blocked = UUID.randomUUID();
            final UUID other = UUID.randomUUID();
            when(failureRepository.findAllAuthServerIds()).thenReturn(Set.of(blocked));
            pollReturns(eventOf(ACTIVATED, 501L, blocked, Instant.now()), eventOf(ACTIVATED, 502L, other, Instant.now()));

            assertThat(service.sync()).isTrue();

            verify(applier, never()).apply(any(), eq(501L), any(), any());
            verify(failureRepository).enqueue(eq(501L), eq(blocked), eq(ACTIVATED), any(), eq(0), any(), isNull());
            verify(applier).apply(eq(ACTIVATED), eq(502L), eq(other), any());
        }

        @Test
        @DisplayName("줄 세우기 뒤에 커서를 저장한다")
        void savesCursorAfterEnqueue() {
            final UUID blocked = UUID.randomUUID();
            when(failureRepository.findAllAuthServerIds()).thenReturn(Set.of(blocked));
            pollReturns(eventOf(ACTIVATED, 601L, blocked, Instant.now()));

            service.sync();

            final InOrder order = inOrder(failureRepository, cursorRepository);
            order.verify(failureRepository).enqueue(eq(601L), any(), any(), any(), anyInt(), any(), any());
            order.verify(cursorRepository).save(any(PollingCursor.class));
        }
    }

    @Nested
    @DisplayName("줄 세우기 자체가 실패하면(DB 장애)")
    class EnqueueFailure {

        @Test
        @DisplayName("커서를 넘기지 않고 그 지점에서 멈추며 sync()는 false다. MEMBERSHIP_SYNC_FAIL ERROR를 남긴다")
        void stopsWithoutAdvancing() {
            final UUID user = UUID.randomUUID();
            pollReturns(eventOf(WITHDRAWN, 701L, user, Instant.now()), eventOf(WITHDRAWN, 702L, UUID.randomUUID(), Instant.now()));
            doThrow(new RuntimeException("DB 장애")).when(applier).apply(any(), eq(701L), any(), any());
            when(failureRepository.enqueue(eq(701L), any(), any(), any(), anyInt(), any(), any()))
                    .thenThrow(new DataAccessResourceFailureException("connection refused"));

            assertThat(service.sync()).isFalse();

            verify(cursorRepository, never()).save(any());
            verify(applier, never()).apply(any(), eq(702L), any(), any());
            verify(pollClient, times(1)).poll(any(), any(), eq(500)); // 재폴링 없이 그 자리에서 멈춤
            assertThat(logs.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getMarkerList()).contains(AlertMarkers.MEMBERSHIP_SYNC_FAIL);
            });
        }
    }

    @Nested
    @DisplayName("이벤트 검증")
    class Validation {

        @Test
        @DisplayName("type null·모르는 type·userId null은 줄이 막힌 유저 것이어도 줄 세우지 않고 커서만 전진한다")
        void skipsInvalidEventsWithoutQueueing() {
            final UUID blocked = UUID.randomUUID();
            when(failureRepository.findAllAuthServerIds()).thenReturn(Set.of(blocked));
            pollReturns(
                    eventOf(null, 801L, blocked, Instant.now()),
                    eventOf("SOME_VERY_LONG_UNKNOWN_EVENT_TYPE_FROM_FUTURE_AS", 802L, blocked, Instant.now()),
                    eventOf(WITHDRAWN, 803L, null, Instant.now()));

            assertThat(service.sync()).isTrue();

            verify(failureRepository, never()).enqueue(any(), any(), any(), any(), anyInt(), any(), any());
            verifyNoInteractions(applier);
            assertThat(lastSavedCursor(3).getSinceId()).isEqualTo(803L);
        }

        @Test
        @DisplayName("id나 changedAt이 없으면 그 지점에서 멈추고 sync()는 false다")
        void stopsOnMissingCursorKeys() {
            pollReturns(eventOf(WITHDRAWN, 901L, UUID.randomUUID(), null));

            assertThat(service.sync()).isFalse();

            verify(cursorRepository, never()).save(any());
            verifyNoInteractions(applier);
            assertThat(logs.list).anySatisfy(event ->
                    assertThat(event.getMarkerList()).contains(AlertMarkers.MEMBERSHIP_SYNC_FAIL));
        }
    }

    @Nested
    @DisplayName("AS 호출 자체가 실패했을 때")
    class PollFailure {

        @Test
        @DisplayName("이번 실행을 중단하고 커서를 건드리지 않으며 sync()는 false다")
        void skipsRunWithoutTouchingCursor() {
            when(pollClient.poll(any(), any(), eq(500))).thenThrow(new ResourceAccessException("connect timed out"));

            assertThat(service.sync()).isFalse();

            verify(cursorRepository, never()).save(any());
            verifyNoInteractions(applier);
        }
    }

    @Nested
    @DisplayName("락")
    class Locking {

        @Test
        @DisplayName("sync()는 MembershipSyncLock 안에서 실행된다")
        void runsInsideLock() {
            final MembershipSyncLock lock = mock(MembershipSyncLock.class);
            final MembershipEventSyncService locked =
                    new MembershipEventSyncService(pollClient, cursorRepository, applier, failureRepository, lock);

            locked.sync();

            verify(lock).callLocked(any());
            verifyNoInteractions(pollClient); // mock 락이 작업을 실행하지 않았으므로 폴링도 없다
        }
    }

    @Nested
    @DisplayName("AS가 hasNext를 줄 때")
    class WithHasNext {

        @Test
        @DisplayName("hasNext=true면 개수가 limit보다 적어도 바로 다음 페이지를 요청한다")
        void followsHasNextTrue() {
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(List.of(activatedEvent(1L)), true))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), false));

            assertThat(service.sync()).isTrue();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("hasNext=false면 개수가 limit과 같아도 다시 요청하지 않는다(빈 페이지를 한 번 더 받지 않음)")
        void stopsOnHasNextFalseEvenWhenPageIsFull() {
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(fullPage(), false))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), false));

            service.sync();

            verify(pollClient, times(1)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("hasNext=true여도 항목이 비어 있으면 멈춘다(커서가 그대로라 같은 요청이 끝없이 반복되는 것 방지)")
        void stopsOnEmptyItemsEvenIfHasNextTrue() {
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), true))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), false));

            service.sync();

            verify(pollClient, times(1)).poll(any(), any(), eq(500));
        }
    }

    @Nested
    @DisplayName("AS가 hasNext를 주지 않을 때(hasNext 추가 전 AS)")
    class WithoutHasNext {

        @Test
        @DisplayName("개수가 limit과 같으면 더 있을 수 있으므로 갱신된 커서로 즉시 재폴링한다")
        void pollsAgainImmediately() {
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(fullPage(), null))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), null));

            service.sync();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("개수가 limit보다 적으면 거기서 끝낸다")
        void stopsWhenPageIsNotFull() {
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(List.of(activatedEvent(1L)), null))
                    .thenReturn(new MembershipEventsPollResponse(List.of(), null));

            service.sync();

            verify(pollClient, times(1)).poll(any(), any(), eq(500));
        }
    }

    private MembershipEventResponse activatedEvent(final long logId) {
        return eventOf(ACTIVATED, logId, UUID.randomUUID(), Instant.EPOCH.plusSeconds(logId));
    }

    /** CLIENT_ACTIVATED 500건(PAGE_LIMIT)짜리 꽉 찬 페이지 */
    private List<MembershipEventResponse> fullPage() {
        final List<MembershipEventResponse> page = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            page.add(activatedEvent(i));
        }
        return page;
    }

    private PollingCursor existingCursor(final Instant since, final Long sinceId) {
        final PollingCursor cursor = PollingCursor.initial(MembershipEventSyncService.POLLER_NAME);
        cursor.advance(since, sinceId);
        return cursor;
    }
}
