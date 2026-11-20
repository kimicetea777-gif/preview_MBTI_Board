package com.board.kimicetea.domain.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 어드민 댓글 통계 계산 트리거 — 어드민 수동 새로고침이 기본이고, 주기적 사전 계산(스케줄)은 설정으로 켠다
 * 주의: 스케줄 갱신은 기본 꺼짐(admin.stats.scheduler-enabled=false) — 사용자가 적은 동안 불필요한 DB 집계를 막기 위함
 * 주의: triggerAsync()는 어드민 수동 새로고침 시 호출 — @Async로 HTTP 응답과 분리된 스레드에서 실행
 * 주의: @Async 메서드에서 AdminCommentStatsService 호출 시 Spring 프록시 경유 → @Transactional 정상 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminCommentStatsScheduler {

    private final AdminCommentStatsService adminCommentStatsService;

    @Value("${admin.stats.scheduler-enabled:false}")
    private boolean schedulerEnabled;

    /** 통계 캐시 갱신 — schedulerEnabled일 때만 동작. 주기는 admin.stats.cache-refresh-delay-ms(기본 30분), 기동 5초 후 선적재 */
    @Scheduled(fixedDelayString = "${admin.stats.cache-refresh-delay-ms:1800000}", initialDelay = 5_000)
    public void refresh() {
        if (!schedulerEnabled) {
            return;
        }
        try {
            log.debug("댓글 통계 캐시 갱신 시작");
            adminCommentStatsService.computeAndCache();
            log.debug("댓글 통계 캐시 갱신 완료");
        } catch (Exception e) {
            log.error("댓글 통계 캐시 갱신 실패 — 다음 주기에 재시도", e);
        }
    }

    /**
     * 어드민 수동 새로고침 — @Async로 즉시 반환 후 백그라운드 계산
     * 주의: AdminCommentController에서 호출 — 외부 빈 호출이므로 @Async 프록시 정상 적용
     */
    @Async
    public void triggerAsync() {
        try {
            log.info("댓글 통계 캐시 수동 갱신 시작");
            adminCommentStatsService.computeAndCache();
            log.info("댓글 통계 캐시 수동 갱신 완료");
        } catch (Exception e) {
            log.error("댓글 통계 캐시 수동 갱신 실패", e);
        }
    }
}
