package com.board.kimicetea.global.membershipsync;

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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipWithdrawalFinalizeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private MembershipWithdrawalFinalizeService service;

    @BeforeEach
    void setUp() {
        service = new MembershipWithdrawalFinalizeService(userRepository, userService);
    }

    private User pendingUserOf(final UUID id) {
        return User.builder().id(id).state(UserState.MEMBERSHIP_WITHDRAWAL_PENDING).build();
    }

    @Nested
    @DisplayName("유예기간이 지난 유저 조회")
    class QueryCutoff {

        @Test
        @DisplayName("MEMBERSHIP_WITHDRAWAL_PENDING 상태로, cutoff는 현재 시각보다 과거여야 한다(32일 유예)")
        void queriesWithPendingStateAndPastCutoff() {
            when(userRepository.findByStateAndMembershipWithdrawnAtBefore(eq(UserState.MEMBERSHIP_WITHDRAWAL_PENDING), any()))
                    .thenReturn(List.of());

            service.finalizeExpiredWithdrawals();

            final ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(userRepository).findByStateAndMembershipWithdrawnAtBefore(eq(UserState.MEMBERSHIP_WITHDRAWAL_PENDING), cutoffCaptor.capture());
            assertThat(cutoffCaptor.getValue()).isBefore(Instant.now());
        }
    }

    @Nested
    @DisplayName("확정 처리")
    class Finalize {

        @Test
        @DisplayName("조회된 유저마다 finalizeMembershipWithdrawal을 호출한다")
        void finalizesEachTarget() {
            final User user1 = pendingUserOf(UUID.randomUUID());
            final User user2 = pendingUserOf(UUID.randomUUID());
            when(userRepository.findByStateAndMembershipWithdrawnAtBefore(eq(UserState.MEMBERSHIP_WITHDRAWAL_PENDING), any()))
                    .thenReturn(List.of(user1, user2));

            service.finalizeExpiredWithdrawals();

            verify(userService).finalizeMembershipWithdrawal(user1.getId());
            verify(userService).finalizeMembershipWithdrawal(user2.getId());
        }

        @Test
        @DisplayName("한 유저 처리 중 예외가 나도 나머지 유저는 계속 처리한다 — 유저 단위 격리")
        void isolatesFailurePerUser() {
            final User failing = pendingUserOf(UUID.randomUUID());
            final User succeeding = pendingUserOf(UUID.randomUUID());
            when(userRepository.findByStateAndMembershipWithdrawnAtBefore(eq(UserState.MEMBERSHIP_WITHDRAWAL_PENDING), any()))
                    .thenReturn(List.of(failing, succeeding));
            doThrow(new RuntimeException("DB 순간 장애"))
                    .when(userService).finalizeMembershipWithdrawal(failing.getId());

            service.finalizeExpiredWithdrawals();

            verify(userService).finalizeMembershipWithdrawal(failing.getId());
            verify(userService).finalizeMembershipWithdrawal(succeeding.getId());
        }
    }
}
