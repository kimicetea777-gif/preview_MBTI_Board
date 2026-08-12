package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.deletionsync.PollingCursor;
import com.board.kimicetea.global.deletionsync.PollingCursorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipEventSyncServiceTest {

    @Mock
    private MembershipEventPollClient pollClient;

    @Mock
    private PollingCursorRepository cursorRepository;

    @Mock
    private UserService userService;

    private MembershipEventSyncService service;

    @BeforeEach
    void setUp() {
        service = new MembershipEventSyncService(pollClient, cursorRepository, userService);
        // 일부 테스트(폴링 자체 실패 등)는 save()를 아예 안 부르므로 lenient — 스텁 미사용 검증 예외 방지
        lenient().when(cursorRepository.save(any(PollingCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MembershipEventResponse eventOf(final String type, final long logId, final UUID userId, final Instant changedAt) {
        return new MembershipEventResponse(type, logId, userId, "TypeButter", changedAt);
    }

    @Nested
    @DisplayName("커서가 없을 때(최초 폴링)")
    class NoCursorYet {

        @Test
        @DisplayName("EPOCH/sinceId 없이 폴링을 호출한다")
        void pollsFromEpoch() {
            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME)).thenReturn(Optional.empty());
            when(pollClient.poll(Instant.EPOCH, null, 500)).thenReturn(new MembershipEventsPollResponse(List.of()));

            service.sync();

            verify(pollClient).poll(Instant.EPOCH, null, 500);
        }
    }

    @Nested
    @DisplayName("이벤트 타입별 디스패치")
    class Dispatch {

        @Test
        @DisplayName("CLIENT_WITHDRAWN은 handleMembershipWithdrawn(authServerId, changedAt)으로 위임하고 커서를 전진시킨다")
        void dispatchesWithdrawn() {
            final UUID authServerId = UUID.randomUUID();
            final Instant changedAt = Instant.parse("2026-08-01T04:00:03.120Z");
            final MembershipEventResponse event = eventOf(MembershipEventResponse.TYPE_CLIENT_WITHDRAWN, 101L, authServerId, changedAt);

            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new MembershipEventsPollResponse(List.of(event)));

            service.sync();

            verify(userService).handleMembershipWithdrawn(authServerId, changedAt);

            final ArgumentCaptor<PollingCursor> cursorCaptor = ArgumentCaptor.forClass(PollingCursor.class);
            verify(cursorRepository).save(cursorCaptor.capture());
            assertThat(cursorCaptor.getValue().getSinceTimestamp()).isEqualTo(changedAt);
            assertThat(cursorCaptor.getValue().getSinceId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("CLIENT_BANNED는 handleMembershipBanned(authServerId)로 위임한다")
        void dispatchesBanned() {
            final UUID authServerId = UUID.randomUUID();
            final MembershipEventResponse event = eventOf(MembershipEventResponse.TYPE_CLIENT_BANNED, 102L, authServerId, Instant.now());

            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new MembershipEventsPollResponse(List.of(event)));

            service.sync();

            verify(userService).handleMembershipBanned(authServerId);
        }

        @Test
        @DisplayName("CLIENT_ACTIVATED는 handleMembershipActivated(authServerId)로 위임한다")
        void dispatchesActivated() {
            final UUID authServerId = UUID.randomUUID();
            final MembershipEventResponse event = eventOf(MembershipEventResponse.TYPE_CLIENT_ACTIVATED, 103L, authServerId, Instant.now());

            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new MembershipEventsPollResponse(List.of(event)));

            service.sync();

            verify(userService).handleMembershipActivated(authServerId);
        }

        @Test
        @DisplayName("알 수 없는 타입은 예외 없이 무시하고 커서만 전진시킨다")
        void ignoresUnknownType() {
            final UUID authServerId = UUID.randomUUID();
            final MembershipEventResponse event = eventOf("UNKNOWN_TYPE", 104L, authServerId, Instant.now());

            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new MembershipEventsPollResponse(List.of(event)));

            service.sync();

            verifyNoInteractions(userService);
            final ArgumentCaptor<PollingCursor> cursorCaptor = ArgumentCaptor.forClass(PollingCursor.class);
            verify(cursorRepository).save(cursorCaptor.capture());
            assertThat(cursorCaptor.getValue().getSinceId()).isEqualTo(104L);
        }
    }

    @Nested
    @DisplayName("AS 호출 자체가 실패했을 때")
    class PollFailure {

        @Test
        @DisplayName("이번 실행을 중단하고 커서를 건드리지 않는다")
        void skipsRunWithoutTouchingCursor() {
            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenThrow(new ResourceAccessException("connect timed out"));

            service.sync();

            verify(cursorRepository, never()).save(any());
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("항목 처리 중 예외가 나면")
    class ItemProcessingFailure {

        @Test
        @DisplayName("실패한 항목 이전까지만 커서를 전진시키고 그 지점에서 멈춘다")
        void stopsAtFailedItemWithoutSkippingPast() {
            final UUID authServerId1 = UUID.randomUUID();
            final UUID authServerId2 = UUID.randomUUID();
            final Instant changedAt1 = Instant.parse("2026-08-01T04:00:00Z");
            final Instant changedAt2 = Instant.parse("2026-08-01T05:00:00Z");
            final MembershipEventResponse event1 = eventOf(MembershipEventResponse.TYPE_CLIENT_WITHDRAWN, 201L, authServerId1, changedAt1);
            final MembershipEventResponse event2 = eventOf(MembershipEventResponse.TYPE_CLIENT_WITHDRAWN, 202L, authServerId2, changedAt2);

            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(List.of(event1, event2)));
            lenient().doThrow(new RuntimeException("DB 순간 장애"))
                    .when(userService).handleMembershipWithdrawn(eq(authServerId2), any());

            service.sync();

            final ArgumentCaptor<PollingCursor> cursorCaptor = ArgumentCaptor.forClass(PollingCursor.class);
            verify(cursorRepository).save(cursorCaptor.capture());
            assertThat(cursorCaptor.getValue().getSinceId()).isEqualTo(201L); // event2는 반영 안 됨
            verify(pollClient).poll(any(), any(), eq(500)); // 재폴링 없이 그 자리에서 멈춤
        }
    }

    @Nested
    @DisplayName("응답 개수가 limit과 같을 때")
    class FullPage {

        @Test
        @DisplayName("더 있을 수 있으므로 갱신된 커서로 즉시 재폴링한다")
        void pollsAgainImmediately() {
            final List<MembershipEventResponse> fullPage = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                fullPage.add(eventOf(MembershipEventResponse.TYPE_CLIENT_ACTIVATED, i, UUID.randomUUID(), Instant.EPOCH.plusSeconds(i)));
            }
            when(cursorRepository.findById(MembershipEventSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new MembershipEventsPollResponse(fullPage))
                    .thenReturn(new MembershipEventsPollResponse(List.of()));

            service.sync();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }
    }

    private PollingCursor existingCursor(final Instant since, final Long sinceId) {
        final PollingCursor cursor = PollingCursor.initial(MembershipEventSyncService.POLLER_NAME);
        cursor.advance(since, sinceId);
        return cursor;
    }
}
