package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * MBTI 검사 문항 엔티티
 * 주의: 쓰기 기능 없음 — 문항은 Flyway 시드로만 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mbti_questions")
public class MbtiQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "category", nullable = false, length = 2)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "is_reverse", nullable = false)
    private boolean isReverse;

    @Column(name = "test_version", nullable = false, length = 10)
    private String testVersion;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** 어드민 문항 수정 — title·category·isReverse만 변경 가능 */
    public void update(final String title, final String category, final Boolean isReverse) {
        this.title = title;
        this.category = category;
        this.isReverse = isReverse;
    }

    /** 어드민 문항 노출 여부 변경 */
    public void changeVisibility(final boolean visible) {
        this.isVisible = visible;
    }
}
