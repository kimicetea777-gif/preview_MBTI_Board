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
            when(pollClient.poll(Instant.EPOCH, null, 500)).thenReturn(new DeletedUsersPollResponse(List.of(), false));

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
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new DeletedUsersPollResponse(List.of(event), false));
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
            when(pollClient.poll(any(), any(), eq(500))).thenReturn(new DeletedUsersPollResponse(List.of(event), false));
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
                    .thenReturn(new DeletedUsersPollResponse(List.of(event1, event2), false));
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
    @DisplayName("AS가 hasNext를 줄 때")
    class WithHasNext {

        @Test
        @DisplayName("hasNext=true면 개수가 limit보다 적어도 바로 다음 페이지를 요청한다")
        void followsHasNextTrue() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(List.of(eventOf(1L, UUID.randomUUID(), Instant.EPOCH)), true))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), false));

            service.sync();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("hasNext=false면 개수가 limit과 같아도 다시 요청하지 않는다(빈 페이지를 한 번 더 받지 않음)")
        void stopsOnHasNextFalseEvenWhenPageIsFull() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(fullPage(), false))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), false));

            service.sync();

            verify(pollClient, times(1)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("hasNext=true여도 항목이 비어 있으면 멈춘다(커서가 그대로라 같은 요청이 끝없이 반복되는 것 방지)")
        void stopsOnEmptyItemsEvenIfHasNextTrue() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), true))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), false));

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
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(fullPage(), null))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), null));

            service.sync();

            verify(pollClient, times(2)).poll(any(), any(), eq(500));
        }

        @Test
        @DisplayName("개수가 limit보다 적으면 거기서 끝낸다")
        void stopsWhenPageIsNotFull() {
            when(cursorRepository.findById(DeletedUserSyncService.POLLER_NAME))
                    .thenReturn(Optional.of(existingCursor(Instant.EPOCH, null)));
            when(pollClient.poll(any(), any(), eq(500)))
                    .thenReturn(new DeletedUsersPollResponse(List.of(eventOf(1L, UUID.randomUUID(), Instant.EPOCH)), null))
                    .thenReturn(new DeletedUsersPollResponse(List.of(), null));

            service.sync();

            verify(pollClient, times(1)).poll(any(), any(), eq(500));
        }
    }

    /** 로컬에 없는 유저 500명(PAGE_LIMIT)짜리 꽉 찬 페이지 — findByAuthServerId는 mock 기본값 Optional.empty() */
    private List<DeletedUserEventResponse> fullPage() {
        final List<DeletedUserEventResponse> page = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            page.add(eventOf(i, UUID.randomUUID(), Instant.EPOCH.plusSeconds(i)));
        }
        return page;
    }

    private PollingCursor existingCursor(final Instant since, final Long sinceId) {
        final PollingCursor cursor = PollingCursor.initial(DeletedUserSyncService.POLLER_NAME);
        cursor.advance(since, sinceId);
        return cursor;
    }
}
