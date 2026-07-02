package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.AdminCommentStatsResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentSummaryResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentVisibilityRequest;
import com.board.kimicetea.domain.comment.service.AdminCommentService;
import com.board.kimicetea.domain.comment.service.AdminCommentStatsScheduler;
import com.board.kimicetea.domain.comment.service.AdminCommentStatsService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 댓글 관리 API 컨트롤러
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 필수
 */
@Tag(name = "Admin - Comment", description = "어드민 댓글 관리 API")
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

        private final AdminCommentService adminCommentService;
        private final AdminCommentStatsService adminCommentStatsService;
        private final AdminCommentStatsScheduler adminCommentStatsScheduler;

        @Operation(summary = "댓글 통계 조회", description = "Redis 캐시에서 즉시 반환합니다. 캐시는 10분마다 자동 갱신되며 최초 기동 5초 후 선적재됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
        })
        @GetMapping("/stats")
        public ResponseEntity<ApiResponse<AdminCommentStatsResponse>> getCommentStats() {
                return ResponseEntity.ok(ApiResponse.ok(adminCommentStatsService.getCachedStats()));
        }

        @Operation(summary = "댓글 통계 캐시 수동 갱신", description = "백그라운드에서 즉시 재계산 후 Redis 캐시를 업데이트합니다. 실제 적용까지 수 초 소요됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "갱신 시작됨"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
        })
        @PostMapping("/stats/refresh")
        public ResponseEntity<ApiResponse<Void>> refreshStats() {
                adminCommentStatsScheduler.triggerAsync();
                return ResponseEntity.accepted().body(ApiResponse.ok("통계 캐시 갱신을 시작했습니다."));
        }

        @Operation(summary = "댓글 목록 조회", description = "숨겨진 댓글을 포함한 전체 목록을 조회합니다. postId·isVisible·authorNickname·sortDir 필터와 페이지네이션을 지원합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
        })
        @GetMapping
        public ResponseEntity<ApiResponse<Page<AdminCommentSummaryResponse>>> getComments(
                        @RequestParam(required = false) final Long postId,
                        @RequestParam(required = false) final Boolean isVisible,
                        @RequestParam(required = false) final String authorNickname,
                        @RequestParam(defaultValue = "DESC") final String sortDir,
                        @RequestParam(defaultValue = "0") final int page,
                        @RequestParam(defaultValue = "20") final int size) {
                return ResponseEntity.ok(ApiResponse.ok(
                                adminCommentService.getComments(postId, isVisible, authorNickname, sortDir,
                                                PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100))))));
        }

        @Operation(summary = "댓글 가시성 변경", description = "댓글을 숨기거나 복구합니다. 숨겨진 댓글도 대상이 됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가시성 변경 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
        })
        @PatchMapping("/{id}/visibility")
        public ResponseEntity<ApiResponse<Void>> changeCommentVisibility(
                        @PathVariable final long id,
                        @Valid @RequestBody final AdminCommentVisibilityRequest request) {
                adminCommentService.changeCommentVisibility(id, request.visible());
                return ResponseEntity.ok(ApiResponse.ok(
                                request.visible() ? "댓글이 복구되었습니다." : "댓글이 숨겨졌습니다."));
        }
}
