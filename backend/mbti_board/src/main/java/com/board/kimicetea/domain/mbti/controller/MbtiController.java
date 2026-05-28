package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestResultResponse;
import com.board.kimicetea.domain.mbti.dto.MbtiTestSubmitRequest;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MBTI 메타데이터 및 검사 API 컨트롤러
 * 주의: GET 엔드포인트는 인증 불필요, POST /test/submit은 JWT 인증 필수
 */
@Tag(name = "MBTI", description = "MBTI 메타데이터 및 검사 API")
@Validated
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

    /**
     * MBTI 단건 조회 — 프론트 상세 페이지 등 단일 유형 참조 데이터용
     */
    @Operation(summary = "MBTI 단건 조회", description = "MBTI ID로 단일 유형 메타데이터를 조회합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 MBTI")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MbtiResponse>> getMbtiById(@PathVariable @Size(max = 6) final String id) {
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.getMbtiById(id)));
    }

    /**
     * MBTI 검사 문항 조회 — 카테고리당 8개씩 랜덤 추출, 총 32문항
     */
    @Operation(summary = "MBTI 검사 문항 조회", description = "카테고리(EI·SN·TF·JP)별 8개씩 랜덤 추출한 32문항을 반환합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/test/questions")
    public ResponseEntity<ApiResponse<List<MbtiQuestionResponse>>> getTestQuestions() {
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
        if (userDetails == null)
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.submitTest(userDetails.userId(), request)));
    }
}
