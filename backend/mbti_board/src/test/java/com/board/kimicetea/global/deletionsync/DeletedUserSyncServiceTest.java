package com.board.kimicetea.global.deletionsync;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
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
class DeletedUserSyncServiceTest {

    @Mock
    private DeletedUserPollClient pollClient;

    @Mock
    private PollingCursorRepository cursorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private DeletedUserSyncService service;

    @BeforeEach
    void setUp() {
        service = new DeletedUserSyncService(pollClient, cursorRepository, userRepository, userService);
        // 일부 테스트(폴링 자체 실패 등)는 save()를 아예 안 부르므로 lenient — 스텁 미사용 검증 예외 방지
        lenient().when(cursorRepository.save(any(PollingCursor.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DeletedUserEventResponse eventOf(final long logId, final UUID userId, final Instant deletedAt) {
        return new DeletedUserEventResponse("ACCOUNT_DELETED", logId, userId, deletedAt);
    }

    @Nested
    @DisplayName("커서가 없을 때(최초 폴링)")
    class NoCursorYet {

        @Test
        @DisplayName("EPOCH/sinceId 없이 폴링을 호출한다")
        void pollsFromEpoch() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME)).thenReturn(Optional.empty());
            when(pollClient.poll(Instant.EPOCH, null, 500)).thenReturn(new DeletedUsersPollResponse(List.of()));

            service.sync();

            verify(pollClient).poll(Instant.EPOCH, null, 500);
        }
    }

    @Nested
    @DisplayName("정상 항목 처리")
    class NormalProcessing {

        @Test
        @DisplayName("로컬 유저가 있으면 forceWithdrawFromAsDeletion을 호출하고 커서를 그 항목으로 전진시킨다")
        void processesAndAdvancesCursor() {
            final UUID authServerId = UUID.randomUUID();
            final UUID localUserId = UUID.randomUUID();
            final Instant deletedAt = Instant.parse("2026-08-01T04:00:03.120Z");
            final DeletedUserEventResponse event = eventOf(101L, authServerId, deletedAt);
            final User localUser = User.builder().id(localUserId).state(UserState.NORMAL).build();

            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new DeletedUsersPollResponse(List.of(event)));
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.of(localUser));

            service.sync();

            verify(userService).forceWithdrawFromAsDeletion(localUserId);

            final ArgumentCaptor<PollingCursor> cursorCaptor = ArgumentCaptor.forClass(PollingCursor.class);
            verify(cursorRepository).save(cursorCaptor.capture());
            assertThat(cursorCaptor.getValue().getSinceTimestamp()).isEqualTo(deletedAt);
            assertThat(cursorCaptor.getValue().getSinceId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("로컬에 없는 유저는 skip하지만 커서는 그대로 전진시킨다")
        void skipsMissingLocalUserButStillAdvancesCursor() {
            final UUID authServerId = UUID.randomUUID();
            final Instant deletedAt = Instant.parse("2026-08-01T04:00:03.120Z");
            final DeletedUserEventResponse event = eventOf(102L, authServerId, deletedAt);

            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new DeletedUsersPollResponse(List.of(event)));
            when(userRepository.findByAuthServerId(authServerId)).thenReturn(Optional.empty());

            service.sync();

            verifyNoInteractions(userService);
            final ArgumentCaptor<PollingCursor> cursorCaptor = ArgumentCaptor.forClass(PollingCursor.class);
            verify(cursorRepository).save(cursorCaptor.capture());
            assertThat(cursorCaptor.getValue().getSinceId()).isEqualTo(102L);
        }
    }

    @Nested
    @DisplayName("AS 호출 자체가 실패했을 때")
    class PollFailure {

        @Test
        @DisplayName("이번 실행을 중단하고 커서를 건드리지 않는다")
        void skipsRunWithoutTouchingCursor() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
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
            final UUID localUserId1 = UUID.randomUUID();
            final Instant deletedAt1 = Instant.parse("2026-08-01T04:00:00Z");
            final Instant deletedAt2 = Instant.parse("2026-08-01T05:00:00Z");
            final DeletedUserEventResponse event1 = eventOf(201L, authServerId1, deletedAt1);
            final DeletedUserEventResponse event2 = eventOf(202L, authServerId2, deletedAt2);
            final User localUser1 = User.builder().id(localUserId1).state(UserState.NORMAL).build();

            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(List.of(event1, event2)));
            when(userRepository.findByAuthServerId(authServerId1)).thenReturn(Optional.of(localUser1));
            when(userRepository.findByAuthServerId(authServerId2)).thenThrow(new RuntimeException("DB 순간 장애"));

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
            final List<DeletedUserEventResponse> fullPage = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                fullPage.add(eventOf(i, UUID.randomUUID(), Instant.EPOCH.plusSeconds(i)));
            }
            when(userRepository.findByAuthServerId(any())).thenReturn(Optional.empty());
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(fullPage))
                    .thenReturn(new DeletedUsersPollResponse(List.of()));

            service.sync();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }
    }

    private PollingCursor existingCursor(final Instant since, final Long sinceId) {
        final PollingCursor cursor = PollingCursor.initial(DeletedUserSyncService.POLLER_NAME);
        cursor.advance(since, sinceId);
        return cursor;
    }
}
