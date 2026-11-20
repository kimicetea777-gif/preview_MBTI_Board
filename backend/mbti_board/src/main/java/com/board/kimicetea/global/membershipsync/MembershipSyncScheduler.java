package com.board.kimicetea.global.membershipsync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 오전 04:15 — DeletedUserSyncScheduler(04:10) 이후. 이벤트 동기화가 완전히 끝난 뒤에만 확정
 * 배치가 돌아야 한다: 그렇지 않으면 "재연결했지만 폴러가 아직 못 가져온 사이 확정 배치가 먼저 도는"
 * 레이스가 생겨 재연결 유저가 영구 차단될 수 있다. 이를 별도 cron 두 개로 시간만 벌려 우연에 맡기지
 * 않고, 하나의 트리거 안에서 순서를 명시적으로 강제한다.
 * <p>
 * 폴링이 끝까지 성공하지 못한 날(AS 장애 등)은 확정 배치를 건너뛴다 — 받지 못한 재연결 이벤트가 있을 수 있고
 * 익명화는 되돌릴 수 없다. 하루 미뤄질 뿐이다. 폴링·확정 배치·점검을 한 락 안에서 돌려 재시도 작업이 끼어들지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipSyncScheduler {

    private final MembershipEventSyncService membershipEventSyncService;
    private final MembershipWithdrawalFinalizeService membershipWithdrawalFinalizeService;
    private final MembershipSyncAlertService membershipSyncAlertService;
    private final MembershipSyncLock lock;

    @Scheduled(cron = "0 15 4 * * *")
    public void syncMembershipEvents() {
        lock.runLocked(() -> {
            final boolean synced = membershipEventSyncService.sync();
            if (synced) {
                membershipWithdrawalFinalizeService.finalizeExpiredWithdrawals();
            } else {
                log.warn("[membership-sync] 폴링이 끝까지 성공하지 못해 오늘 확정 배치를 건너뜀");
            }
            membershipSyncAlertService.checkDaily(synced);
        });
    }
}
