package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.repository.UserRepository;
import com.board.kimicetea.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * AS 연결 해제(CLIENT_WITHDRAWN) 후 유예기간이 지나도록 재연결이 없는 유저를 확정 익명화한다.
 * 주의: 유예기간 상수는 AS BatchPolicy.WITHDRAWN_MEMBERSHIP_PURGE_GRACE_PERIOD(현재 30일)와 반드시
 *       맞출 것 — 두 값이 어긋나면 재연결 유저가 영구 차단되거나 유예가 과도해질 수 있다. 정확히 30일이
 *       아니라 32일(30일 + 2일 안전 마진)로 잡아, MembershipEventSyncService가 먼저 끝나 재연결
 *       (CLIENT_ACTIVATED)을 반영할 시간을 벌어준다 — MembershipSyncScheduler가 두 단계를 이 순서로 강제.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipWithdrawalFinalizeService {

    private static final Duration GRACE_PERIOD = Duration.ofDays(32);

    private final UserRepository userRepository;
    private final UserService userService;

    /** 유저 단위로 실패를 격리 — 한 유저 처리 실패가 나머지 확정 처리를 막지 않도록 함 */
    public void finalizeExpiredWithdrawals() {
        final Instant cutoff = Instant.now().minus(GRACE_PERIOD);
        final List<User> targets =
                userRepository.findByStateAndMembershipWithdrawnAtBefore(UserState.MEMBERSHIP_WITHDRAWAL_PENDING, cutoff);

        for (final User user : targets) {
            try {
                userService.finalizeMembershipWithdrawal(user.getId());
            } catch (final RuntimeException e) {
                log.error("멤버십 탈퇴 확정 처리 실패 (userId={}) — 이 유저만 스킵, 나머지 계속 처리", user.getId(), e);
            }
        }
    }
}
