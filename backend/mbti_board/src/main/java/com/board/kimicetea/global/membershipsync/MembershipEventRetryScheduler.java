package com.board.kimicetea.global.membershipsync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [역할] 10초마다 줄 선 멤버십 이벤트를 다시 적용하고, 큐가 비면 MEMBERSHIP_SYNC_FAIL 알림을 다시 켠다.
 *        fixedDelay라 이전 실행이 끝난 뒤에 다음 실행이 잡혀 겹치지 않는다.
 * [주의사항] 폴링(04:15)이 락을 잡고 있으면 이번 틱은 기다리지 않고 건너뛴다.
 *           실행 실패는 WARN만 남긴다 — 같은 마커의 ERROR로 남기면 DB가 잠깐 끊긴 것만으로 메일이 나가고 타입이 음소거돼,
 *           그 뒤의 진짜 "5회 실패" 메일이 나가지 않는다. 처리 못 한 행은 그대로 남아 다음 틱에 다시 시도된다.
 *           단일 인스턴스 전제. 통합 테스트는 membership-event.retry.enabled=false로 끈다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "membership-event.retry.enabled", havingValue = "true", matchIfMissing = true)
public class MembershipEventRetryScheduler {

    private static final long RETRY_INTERVAL_MS = 10_000;
    /** 기동 직후 다른 초기화와 겹치지 않게 조금 늦게 시작 */
    private static final long INITIAL_DELAY_MS = 30_000;

    private final MembershipEventRetryService retryService;
    private final MembershipSyncAlertService alertService;
    private final MembershipSyncLock lock;

    @Scheduled(fixedDelay = RETRY_INTERVAL_MS, initialDelay = INITIAL_DELAY_MS)
    public void retryEvents() {
        try {
            lock.tryRunLocked(() -> {
                retryService.retryDue();
                alertService.reenableIfDrained();
            });
        } catch (final RuntimeException e) {
            log.warn("[membership-retry] 재시도 작업 실행 실패 — 다음 틱에 다시 시도", e);
        }
    }
}
