package com.board.kimicetea.domain.mbti.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FN 질문 커스텀 선택지 엔티티 — fun_question_options 테이블 매핑
 * 주의: optionText만 어드민 수정 가능, 선택지 추가·삭제·순서 변경은 Flyway 시드로 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fun_question_options")
public class FunQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FunQuestion question;

    @Column(name = "option_text", nullable = false, length = 100)
    private String optionText;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    /** 어드민 선택지 텍스트 수정 */
    public void updateText(final String optionText) {
        this.optionText = optionText;
    }
}
