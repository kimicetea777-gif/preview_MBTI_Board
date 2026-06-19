package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FN 응답 사전 집계 엔티티 — fun_response_counts 테이블 매핑
 * 주의: 직접 저장하지 않고 레포지토리 upsert 네이티브 쿼리로만 카운트 갱신
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fun_response_counts")
public class FunResponseCount {

    @EmbeddedId
    private FunResponseCountId id;

    @Column(name = "count", nullable = false)
    private Long count;
}
