package com.board.kimicetea.domain.mbti.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 어드민 FUN 문항 생성 요청 DTO — 제목과 선택지를 함께 등록
 * 주의: testVersion은 요청에서 받지 않고 서비스에서 고정값("v1.0")으로 채워짐
 */
public record AdminFunQuestionCreateRequest(
        @NotBlank @Size(max = 300) String title,
        @NotNull @Size(min = 2, message = "선택지는 최소 2개 이상이어야 합니다.")
        List<@Valid AdminFunOptionCreateRequest> options
) {}
