package com.board.kimicetea.global.image.sync;

import com.board.kimicetea.global.alert.AlertMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 10초마다 이미지 확정·정리 워커 실행. fixedDelay라 이전 실행이 끝난 뒤에 다음 실행이 잡혀 겹치지 않는다.
 * 주의: 서버 인스턴스가 하나라는 전제 — 여러 대로 늘리면 두 워커가 같은 행을 집지 않도록 선점이 필요하다.
 * 주의: 통합 테스트는 image-server.sync.enabled=false로 끈다(테스트가 직접 상태를 검증하는 사이 워커가 끼어들지 않게).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image-server.sync.enabled", havingValue = "true", matchIfMissing = true)
public class PostImageSyncScheduler {

    private final PostImageSyncService postImageSyncService;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void syncImages() {
        try {
            postImageSyncService.syncDue();
        } catch (RuntimeException e) {
            // DB 장애 등 워커 자체가 돌지 못한 경우 — 처리 못 한 행은 그대로 남아 다음 실행에서 다시 시도된다
            log.error(AlertMarkers.IMAGE_SYNC_FAIL, "[image-sync] 워커 실행 실패", e);
        }
    }
}
