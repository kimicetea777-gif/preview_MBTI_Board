package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiTestResultResponse;
import com.board.kimicetea.domain.mbti.dto.QuestionResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestSubmitRequest;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MBTI 검사 API 컨트롤러 — 메타데이터 표시 데이터는 프론트 정적, 검사 기능만 제공
 * 주의: GET /test/questions 인증 불필요, POST /test/submit은 JWT 인증 필수
 */
@Tag(name = "MBTI", description = "MBTI 검사 API")
@Validated
@RestController
@RequestMapping("/api/mbti")
@RequiredArgsConstructor
public class MbtiController {

    private final MbtiService mbtiService;

    /**
     * MBTI 검사 문항 조회 — MBTI 32문항 + FN 3문항 혼합, 총 35문항
     * 주의: type 필드로 MBTI / FUN 구분 (Discriminated Union)
     */
    @Operation(summary = "MBTI 검사 문항 조회", description = "MBTI 32문항 + FN 재미 3문항을 섞어 반환합니다. type 필드로 구분. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/test/questions")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getTestQuestions() {
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.getTestQuestions()));
    }

    /**
     * MBTI 검사 결과 제출 — 점수 범위 검증 후 MBTI 재도출, 이력 저장 및 유저 MBTI 업데이트
     * 주의: 회원 전용 (JWT 인증 필수)
     */
    @Operation(summary = "MBTI 검사 결과 제출", description = "4개 지표 점수를 제출하면 서버가 MBTI를 판정하고 검사 이력을 저장합니다. 회원 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "점수 범위 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/test/submit")
    public ResponseEntity<ApiResponse<MbtiTestResultResponse>> submitTest(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final MbtiTestSubmitRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.submitTest(userDetails.userId(), request)));
    }
}
