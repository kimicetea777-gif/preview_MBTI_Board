package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MBTI 16개 유형 + NONE 마스터 데이터 — FK 참조 전용 (표시 데이터는 프론트 정적)
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mbti_metadata")
public class MbtiMetadata {

    @Id
    @Column(name = "id", length = 6)
    private String id;

    public String getId() {
        return id;
    }

    /** MBTI 미설정 항목 여부 — MbtiVerified.DEFAULT 결정에 사용 */
    public boolean isNone() {
        return "NONE".equals(this.id);
    }
}
