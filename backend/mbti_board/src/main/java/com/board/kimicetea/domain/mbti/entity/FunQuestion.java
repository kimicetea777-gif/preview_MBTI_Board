package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * FN(재미) 질문 엔티티 — fun_questions 테이블 매핑
 * 주의: 문항 내용은 원래 Flyway 시드 전용이었으나, 어드민 생성 기능(AdminFunQuestionService.createFunQuestion)
 *       추가로 builder()를 통한 생성도 허용. 가시성(isVisible)은 어드민에서 변경 가능
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fun_questions")
public class FunQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "test_version", nullable = false, length = 10)
    private String testVersion;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<FunQuestionOption> options;

    /** 어드민 FUN 문항 제목 수정 */
    public void updateTitle(final String title) {
        this.title = title;
    }

    /** 어드민 FUN 문항 노출 여부 변경 */
    public void changeVisibility(final boolean visible) {
        this.isVisible = visible;
    }
}
