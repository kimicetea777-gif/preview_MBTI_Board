package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiResponse;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MBTI 메타데이터 API 컨트롤러
 * 주의: 모든 엔드포인트는 인증 불필요 (정적 참조 데이터)
 */
@Tag(name = "MBTI", description = "MBTI 메타데이터 API")
@RestController
@RequestMapping("/api/mbti")
@RequiredArgsConstructor
public class MbtiController {

    private final MbtiService mbtiService;

    /**
     * 전체 MBTI 목록 조회 — 프론트 셀렉트박스 등 참조 데이터용
     */
    @Operation(summary = "MBTI 목록 조회", description = "전체 MBTI 유형 목록을 조회합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MbtiResponse>>> getMbtiList() {
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.getAllMbti()));
    }
}
