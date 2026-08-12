package com.board.kimicetea.global.membershipsync;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 오전 04:15 — DeletedUserSyncScheduler(04:10) 이후. 이벤트 동기화가 완전히 끝난 뒤에만 확정
 * 배치가 돌아야 한다: 그렇지 않으면 "재연결했지만 폴러가 아직 못 가져온 사이 확정 배치가 먼저 도는"
 * 레이스가 생겨 재연결 유저가 영구 차단될 수 있다. 이를 별도 cron 두 개로 시간만 벌려 우연에 맡기지
 * 않고, 하나의 트리거 안에서 순서를 명시적으로 강제한다.
 */
@Component
@RequiredArgsConstructor
public class MembershipSyncScheduler {

    private final MembershipEventSyncService membershipEventSyncService;
    private final MembershipWithdrawalFinalizeService membershipWithdrawalFinalizeService;

    @Scheduled(cron = "0 15 4 * * *")
    public void syncMembershipEvents() {
        membershipEventSyncService.sync();
        membershipWithdrawalFinalizeService.finalizeExpiredWithdrawals();
    }
}
