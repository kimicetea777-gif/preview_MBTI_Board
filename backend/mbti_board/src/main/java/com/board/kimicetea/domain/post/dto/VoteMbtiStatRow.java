package com.board.kimicetea.domain.post.dto;

/**
 * 투표 선택지별 MBTI 통계 집계 네이티브 쿼리 프로젝션
 * 주의: 컬럼 alias가 getter 이름과 대소문자 무관하게 매핑됨
 */
public interface VoteMbtiStatRow {
    Long getOptionId();
    String getMbtiId();
    Long getCount();
}
