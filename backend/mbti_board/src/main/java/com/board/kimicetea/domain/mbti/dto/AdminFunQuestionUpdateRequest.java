package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 어드민 FUN 문항 수정 요청 DTO — 제목과 선택지 텍스트만 변경 가능
 * 주의: 선택지 추가·삭제·순서 변경 불가 (Flyway 시드로만 관리)
 */
public record AdminFunQuestionUpdateRequest(
        @NotBlank @Size(max = 300) String title,
        @NotNull @Size(min = 1) List<@Valid AdminFunOptionUpdateRequest> options
) {}
