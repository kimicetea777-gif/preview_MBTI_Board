package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.FunResponseSubmitRequest;
import com.board.kimicetea.domain.mbti.dto.FunStatsResponse;
import com.board.kimicetea.domain.mbti.service.FunService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FN(재미) 질문 응답·통계 API 컨트롤러
 * 주의: 두 엔드포인트 모두 인증 불필요 — 비회원 포함
 */
@Tag(name = "FUN", description = "재미 질문 응답·통계 API")
@Validated
@RestController
@RequestMapping("/api/mbti/fun")
@RequiredArgsConstructor
public class FunController {

    private final FunService funService;

    @Operation(summary = "재미 질문 응답 저장", description = "검사 결과 화면 진입 시 선택한 FN 질문 응답을 저장합니다. 인증 불필요, 비회원 포함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
    })
    @PostMapping("/responses")
    public ResponseEntity<ApiResponse<Void>> submitResponses(
            @Valid @RequestBody final FunResponseSubmitRequest request) {
        funService.submitResponses(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "재미 질문 전체 통계 조회", description = "전체 FN 질문에 대한 선택지별·MBTI별 응답 통계를 반환합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<FunStatsResponse>>> getAllStats() {
        return ResponseEntity.ok(ApiResponse.ok(funService.getAllStats()));
    }
}
