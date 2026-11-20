package com.board.kimicetea.global.membershipsync;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 04:15 멤버십 동기화 스케줄러와 10초 재시도 스케줄러의 호출 순서·락·실패 처리 */
@ExtendWith(MockitoExtension.class)
class MembershipSyncSchedulerTest {

    @Mock
    private MembershipEventSyncService syncService;

    @Mock
    private MembershipWithdrawalFinalizeService finalizeService;

    @Mock
    private MembershipSyncAlertService alertService;

    @Mock
    private MembershipEventRetryService retryService;

    @Nested
    @DisplayName("MembershipSyncScheduler (04:15)")
    class DailySync {

        private MembershipSyncScheduler scheduler;

        @BeforeEach
        void setUp() {
            scheduler = new MembershipSyncScheduler(syncService, finalizeService, alertService, new MembershipSyncLock());
        }

        @Test
        @DisplayName("폴링이 성공하면 sync → 확정 배치 → checkDaily(true) 순서로 호출한다")
        void finalizesAfterSuccessfulSync() {
            when(syncService.sync()).thenReturn(true);

            scheduler.syncMembershipEvents();

            final InOrder order = inOrder(syncService, finalizeService, alertService);
            order.verify(syncService).sync();
            order.verify(finalizeService).finalizeExpiredWithdrawals();
            order.verify(alertService).checkDaily(true);
        }

        @Test
        @DisplayName("폴링이 끝까지 성공하지 못하면 확정 배치를 건너뛰고 checkDaily(false)를 호출한다")
        void skipsFinalizeWhenSyncFailed() {
            when(syncService.sync()).thenReturn(false);

            scheduler.syncMembershipEvents();

            verify(finalizeService, never()).finalizeExpiredWithdrawals();
            verify(alertService).checkDaily(false);
        }

        @Test
        @DisplayName("락 안에서 실행된다")
        void runsInsideLock() {
            final MembershipSyncLock lock = mock(MembershipSyncLock.class);
            new MembershipSyncScheduler(syncService, finalizeService, alertService, lock).syncMembershipEvents();

            verify(lock).runLocked(any(Runnable.class));
            verifyNoInteractions(syncService, finalizeService, alertService);
        }
    }

    @Nested
    @DisplayName("MembershipEventRetryScheduler (10초)")
    class RetryTick {

        @Test
        @DisplayName("retryDue() 다음에 reenableIfDrained()를 호출한다")
        void reenablesAfterRetry() {
            new MembershipEventRetryScheduler(retryService, alertService, new MembershipSyncLock()).retryEvents();

            final InOrder order = inOrder(retryService, alertService);
            order.verify(retryService).retryDue();
            order.verify(alertService).reenableIfDrained();
        }

        @Test
        @DisplayName("retryDue()가 예외를 던져도 밖으로 전파하지 않고, 그 틱에는 reenableIfDrained()를 부르지 않는다")
        void swallowsRetryFailure() {
            doThrow(new IllegalStateException("db down")).when(retryService).retryDue();
            final MembershipEventRetryScheduler scheduler =
                    new MembershipEventRetryScheduler(retryService, alertService, new MembershipSyncLock());

            assertThatCode(scheduler::retryEvents).doesNotThrowAnyException();

            verify(alertService, never()).reenableIfDrained();
        }

        @Test
        @DisplayName("락을 못 잡으면(폴링 중) retryDue()를 호출하지 않는다")
        void skipsWhenLockBusy() throws InterruptedException {
            final MembershipSyncLock lock = new MembershipSyncLock();
            final MembershipEventRetryScheduler scheduler = new MembershipEventRetryScheduler(retryService, alertService, lock);
            final CountDownLatch held = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final Thread holder = new Thread(() -> lock.runLocked(() -> {
                held.countDown();
                try {
                    release.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            holder.start();
            held.await();

            scheduler.retryEvents();

            release.countDown();
            holder.join();
            verifyNoInteractions(retryService, alertService);
        }
    }
}
