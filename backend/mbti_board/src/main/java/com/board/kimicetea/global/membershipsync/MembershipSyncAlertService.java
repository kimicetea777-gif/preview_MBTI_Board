package com.board.kimicetea.global.membershipsync;

import com.board.kimicetea.global.alert.AlertFlagRegistry;
import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.alert.AlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * [역할] 멤버십 동기화 알림 정책 — 큐가 비면 MEMBERSHIP_SYNC_FAIL을 다시 켜고(reenableIfDrained),
 *        04:15에 폴링 실패·장기 방치를 점검해 MEMBERSHIP_SYNC_STALE로 알린다(checkDaily).
 * [주의사항] AlertingSmtpAppender는 메일을 한 번 보내면 그 타입을 서버 재시작 전까지 음소거한다. 그대로 두면 다음 장애를
 *           알리지 못하므로, 상황이 풀렸을 때 여기서 다시 켠다 — 장애 한 건에 종류별 메일 한 번.
 *           AlertFlagRegistry는 Logback(스프링 컨텍스트 밖)이 읽는 static 플래그라 직접 부른다.
 * 설계: Document/membership_event_retry_plan.md "알림"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipSyncAlertService {

    /** 이보다 오래 줄 서 있는 이벤트가 있으면 장기 방치 알림 */
    static final Duration STALE_THRESHOLD = Duration.ofHours(24);

    private final MembershipEventFailureRepository failureRepository;

    /** MEMBERSHIP_SYNC_FAIL이 음소거 상태이고 큐가 비었으면 다시 켠다 — 재시도 스케줄러가 매 틱 끝에 호출 */
    public void reenableIfDrained() {
        if (AlertFlagRegistry.isEnabled(AlertType.MEMBERSHIP_SYNC_FAIL)) {
            return; // 평상시에는 쿼리하지 않는다
        }
        if (failureRepository.count() == 0) {
            AlertFlagRegistry.unmute(AlertType.MEMBERSHIP_SYNC_FAIL);
            log.info("[membership-retry] 재시도 큐가 비어 MEMBERSHIP_SYNC_FAIL 알림을 다시 켰다");
        }
    }

    /**
     * 04:15 작업 끝 점검 — 폴링이 끝까지 성공하지 못했거나 오래된 행이 있으면 MEMBERSHIP_SYNC_STALE ERROR,
     * 둘 다 아니면 그 타입을 다시 켠다. 연속 실패 일수는 세지 않는다(이어지는 동안은 음소거로 첫날 한 번만 메일).
     * 주의: 점검 자체가 실패해도(DB 장애) 04:15 작업의 다른 결과를 덮지 않도록 WARN만 남긴다.
     */
    public void checkDaily(final boolean synced) {
        try {
            final long staleCount = failureRepository.countByCreatedAtBefore(Instant.now().minus(STALE_THRESHOLD));
            if (!synced || staleCount > 0) {
                log.error(AlertMarkers.MEMBERSHIP_SYNC_STALE,
                        "[membership-sync] 점검: 폴링 {} / {}시간 넘게 반영되지 않은 멤버십 이벤트 {}건{}",
                        synced ? "정상" : "실패(오늘 확정 배치 건너뜀)", STALE_THRESHOLD.toHours(), staleCount,
                        staleCount > 0 ? " — membership_event_failures 확인" : "");
                return;
            }
            if (!AlertFlagRegistry.isEnabled(AlertType.MEMBERSHIP_SYNC_STALE)) {
                AlertFlagRegistry.unmute(AlertType.MEMBERSHIP_SYNC_STALE);
                log.info("[membership-sync] 점검 정상 — MEMBERSHIP_SYNC_STALE 알림을 다시 켰다");
            }
        } catch (final RuntimeException e) {
            log.warn("[membership-sync] 장기 방치 점검 실패 — 다음 04:15에 다시 점검", e);
        }
    }
}
