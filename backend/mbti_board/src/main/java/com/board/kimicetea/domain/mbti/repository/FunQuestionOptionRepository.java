package com.board.kimicetea.domain.mbti.repository;

import com.board.kimicetea.domain.mbti.entity.FunQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * FN 질문 선택지 레포지토리
 * 주의: 어드민 문항 생성 시 saveAll()로 일괄 저장 — 이후 개별 추가·삭제는 불가
 */
public interface FunQuestionOptionRepository extends JpaRepository<FunQuestionOption, Long> {
}
