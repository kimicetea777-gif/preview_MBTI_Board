package com.board.kimicetea.domain.mbti.dto;

/**
 * MBTI + FN 혼합 질문 응답 공통 인터페이스
 * 주의: type 필드는 각 record의 컴포넌트로 직접 선언 — Jackson 어노테이션 주입 불사용
 */
public sealed interface QuestionResponse
        permits MbtiQuestionResponse, FunQuestionResponse {}
