package com.board.kimicetea.domain.comment.controller;

import com.board.kimicetea.domain.comment.dto.AdminCommentSummaryResponse;
import com.board.kimicetea.domain.comment.dto.AdminCommentVisibilityRequest;
import com.board.kimicetea.domain.comment.service.AdminCommentService;
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

    @Operation(summary = "댓글 목록 조회", description = "숨겨진 댓글을 포함한 전체 목록을 조회합니다. postId·isVisible 필터와 페이지네이션을 지원합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCommentSummaryResponse>>> getComments(
            @RequestParam(required = false) final Long postId,
            @RequestParam(required = false) final Boolean isVisible,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCommentService.getComments(postId, isVisible,
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
        final String message = request.visible() ? "댓글이 복구되었습니다." : "댓글이 숨겨졌습니다.";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
