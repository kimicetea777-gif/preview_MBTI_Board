package com.board.kimicetea.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 어드민 게시글 통계 사전 계산 스케줄러 — 2시간마다 DB 집계 결과를 Redis에 적재
 * 주의: triggerAsync()는 어드민 수동 새로고침 시 호출 — @Async로 HTTP 응답과 분리된 스레드에서 실행
 * 주의: @Async 메서드에서 AdminPostStatsService 호출 시 Spring 프록시 경유 → @Transactional 정상 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPostStatsScheduler {

    private final AdminPostStatsService adminPostStatsService;

    /** 통계 캐시 갱신 — 주기는 admin.stats.cache-refresh-delay-ms(기본 30분), 기동 5초 후 선적재 */
    @Scheduled(fixedDelayString = "${admin.stats.cache-refresh-delay-ms:1800000}", initialDelay = 5_000)
    public void refresh() {
        try {
            log.debug("게시글 통계 캐시 갱신 시작");
            adminPostStatsService.computeAndCache();
            log.debug("게시글 통계 캐시 갱신 완료");
        } catch (Exception e) {
            log.error("게시글 통계 캐시 갱신 실패 — 다음 주기에 재시도", e);
        }
    }

    /**
     * 어드민 수동 새로고침 — @Async로 즉시 반환 후 백그라운드 계산
     * 주의: AdminPostController에서 호출 — 외부 빈 호출이므로 @Async 프록시 정상 적용
     */
    @Async
    public void triggerAsync() {
        try {
            log.info("게시글 통계 캐시 수동 갱신 시작");
            adminPostStatsService.computeAndCache();
            log.info("게시글 통계 캐시 수동 갱신 완료");
        } catch (Exception e) {
            log.error("게시글 통계 캐시 수동 갱신 실패", e);
        }
    }
}
