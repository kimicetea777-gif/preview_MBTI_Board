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
}
