package com.board.kimicetea.global.common;

/**
 * 어드민 통계 Redis 캐시 정책 — 4개 통계 서비스(유저·게시글·댓글·MBTI 검사)가 공유
 * 주의: TTL은 application.yaml의 admin.stats.cache-refresh-delay-ms(현재 2시간)보다 반드시 길어야 함
 *       짧으면 스케줄러 갱신 전에 캐시가 만료돼 첫 조회가 DB를 직접 집계함 (AdminStatsCachePolicyTest가 검증)
 */
public final class AdminStatsCachePolicy {

    private AdminStatsCachePolicy() {
    }

    /** 갱신 주기(2시간)의 2배 — 스케줄러가 한 번 실패해도 캐시가 유지되는 안전망 */
    public static final long TTL_HOURS = 4L;
}
