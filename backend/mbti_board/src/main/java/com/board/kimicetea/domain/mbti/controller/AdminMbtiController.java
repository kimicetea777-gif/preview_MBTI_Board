package com.board.kimicetea.domain.mbti.controller;

import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminFunQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionCreateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionResponse;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionUpdateRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiQuestionVisibilityRequest;
import com.board.kimicetea.domain.mbti.dto.AdminMbtiTestStatsResponse;
import com.board.kimicetea.domain.mbti.service.AdminFunQuestionService;
import com.board.kimicetea.domain.mbti.service.AdminMbtiQuestionService;
import com.board.kimicetea.domain.mbti.service.AdminMbtiTestStatsScheduler;
import com.board.kimicetea.domain.mbti.service.AdminMbtiTestStatsService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 어드민 MBTI 검사 통계 API 컨트롤러
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 필수
 */
@Tag(name = "Admin - Mbti", description = "어드민 MBTI 검사 통계 API")
@RestController
@RequestMapping("/api/admin/mbti")
@RequiredArgsConstructor
public class AdminMbtiController {

    private final AdminMbtiTestStatsService adminMbtiTestStatsService;
    private final AdminMbtiTestStatsScheduler adminMbtiTestStatsScheduler;
    private final AdminMbtiQuestionService adminMbtiQuestionService;
    private final AdminFunQuestionService adminFunQuestionService;

    @Operation(summary = "MBTI 검사 통계 조회", description = "Redis 캐시에서 즉시 반환합니다. 캐시는 10분마다 자동 갱신되며 최초 기동 5초 후 선적재됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminMbtiTestStatsResponse>> getMbtiTestStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminMbtiTestStatsService.getCachedStats()));
    }

    @Operation(summary = "MBTI 검사 통계 캐시 수동 갱신", description = "백그라운드에서 즉시 재계산 후 Redis 캐시를 업데이트합니다. 실제 적용까지 수 초 소요됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "갱신 시작됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping("/stats/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshStats() {
        adminMbtiTestStatsScheduler.triggerAsync();
        return ResponseEntity.accepted().body(ApiResponse.ok("통계 캐시 갱신을 시작했습니다."));
    }

    @Operation(summary = "MBTI 전체 문항 조회", description = "is_visible 여부와 관계없이 모든 문항을 카테고리·ID 오름차순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<AdminMbtiQuestionResponse>>> getAllQuestions() {
        return ResponseEntity.ok(ApiResponse.ok(adminMbtiQuestionService.getAllQuestions()));
    }

    @Operation(summary = "MBTI 문항 생성", description = "새 MBTI 문항을 등록합니다. testVersion은 항상 v1.0으로 고정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping("/questions")
    public ResponseEntity<ApiResponse<AdminMbtiQuestionResponse>> createQuestion(
            @Valid @RequestBody final AdminMbtiQuestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminMbtiQuestionService.createQuestion(request)));
    }

    @Operation(summary = "MBTI 문항 수정", description = "문항의 제목·카테고리·역방향 여부를 수정합니다. testVersion은 변경 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문항 없음")
    })
    @PatchMapping("/questions/{id}")
    public ResponseEntity<ApiResponse<Void>> updateQuestion(
            @PathVariable final Long id,
            @Valid @RequestBody final AdminMbtiQuestionUpdateRequest request) {
        adminMbtiQuestionService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "MBTI 문항 노출 여부 변경", description = "문항을 숨기거나 다시 표시합니다. 숨긴 문항은 검사 출제에서 제외됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문항 없음")
    })
    @PatchMapping("/questions/{id}/visibility")
    public ResponseEntity<ApiResponse<Void>> changeQuestionVisibility(
            @PathVariable final Long id,
            @Valid @RequestBody final AdminMbtiQuestionVisibilityRequest request) {
        adminMbtiQuestionService.changeVisibility(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "FUN 전체 문항 조회", description = "is_visible 여부와 관계없이 모든 FUN 문항을 ID 오름차순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping("/fun-questions")
    public ResponseEntity<ApiResponse<List<AdminFunQuestionResponse>>> getAllFunQuestions() {
        return ResponseEntity.ok(ApiResponse.ok(adminFunQuestionService.getAllFunQuestions()));
    }

    @Operation(summary = "FUN 문항 생성", description = "새 FUN 문항과 선택지를 함께 등록합니다. testVersion은 항상 v1.0으로 고정됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping("/fun-questions")
    public ResponseEntity<ApiResponse<AdminFunQuestionResponse>> createFunQuestion(
            @Valid @RequestBody final AdminFunQuestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminFunQuestionService.createFunQuestion(request)));
    }

    @Operation(summary = "FUN 문항 수정", description = "FUN 문항의 제목과 선택지 텍스트를 수정합니다. 선택지 추가·삭제·순서 변경은 불가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문항 또는 선택지 없음")
    })
    @PatchMapping("/fun-questions/{id}")
    public ResponseEntity<ApiResponse<Void>> updateFunQuestion(
            @PathVariable final Long id,
            @Valid @RequestBody final AdminFunQuestionUpdateRequest request) {
        adminFunQuestionService.updateFunQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "FUN 문항 노출 여부 변경", description = "FUN 문항을 숨기거나 다시 표시합니다. 숨긴 문항은 검사 출제에서 제외됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문항 없음")
    })
    @PatchMapping("/fun-questions/{id}/visibility")
    public ResponseEntity<ApiResponse<Void>> changeFunQuestionVisibility(
            @PathVariable final Long id,
            @Valid @RequestBody final AdminFunQuestionVisibilityRequest request) {
        adminFunQuestionService.changeVisibility(id, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
