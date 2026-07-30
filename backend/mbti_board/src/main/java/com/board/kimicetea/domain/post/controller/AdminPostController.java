package com.board.kimicetea.domain.post.controller;

import com.board.kimicetea.domain.post.dto.AdminPostPinRequest;
import com.board.kimicetea.domain.post.dto.AdminPostStatsResponse;
import com.board.kimicetea.domain.post.dto.AdminPostSummaryResponse;
import com.board.kimicetea.domain.post.dto.AdminPostVisibilityRequest;
import com.board.kimicetea.domain.post.dto.PostDetailResponse;
import com.board.kimicetea.domain.post.service.AdminPostService;
import com.board.kimicetea.domain.post.service.AdminPostStatsScheduler;
import com.board.kimicetea.domain.post.service.AdminPostStatsService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 게시글 관리 API 컨트롤러
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 필수
 */
@Tag(name = "Admin - Post", description = "어드민 게시글 관리 API")
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;
    private final AdminPostStatsService adminPostStatsService;
    private final AdminPostStatsScheduler adminPostStatsScheduler;

    @Operation(summary = "게시글 통계 조회", description = "Redis 캐시에서 즉시 반환합니다. 캐시는 10분마다 자동 갱신되며 최초 기동 5초 후 선적재됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminPostStatsResponse>> getPostStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminPostStatsService.getCachedStats()));
    }

    @Operation(summary = "게시글 통계 캐시 수동 갱신", description = "백그라운드에서 즉시 재계산 후 Redis 캐시를 업데이트합니다. 실제 적용까지 수 초 소요됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "갱신 시작됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping("/stats/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshStats() {
        adminPostStatsScheduler.triggerAsync();
        return ResponseEntity.accepted().body(ApiResponse.ok("통계 캐시 갱신을 시작했습니다."));
    }

    @Operation(summary = "게시글 목록 조회", description = "숨겨진 게시글을 포함한 전체 목록을 조회합니다. typeCode·isVisible·authorNickname 필터, sortDir 정렬, 페이지네이션을 지원합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminPostSummaryResponse>>> getPosts(
            @RequestParam(required = false) final String typeCode,
            @RequestParam(required = false) final Boolean isVisible,
            @RequestParam(required = false) final String authorNickname,
            @RequestParam(defaultValue = "DESC") final String sortDir,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminPostService.getPosts(typeCode, isVisible, authorNickname, sortDir,
                        PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100))))));
    }

    @Operation(summary = "게시글 단건 조회", description = "숨겨진 게시글을 포함해 상세 내용을 조회합니다. viewCount는 증가하지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(@PathVariable final long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminPostService.getPost(id)));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 영구 삭제합니다. 연결된 댓글도 CASCADE 삭제됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable final long id) {
        adminPostService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.ok("게시글이 삭제되었습니다."));
    }

    @Operation(summary = "게시글 가시성 변경", description = "게시글을 숨기거나 복구합니다. 숨겨진 게시글도 대상이 됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가시성 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<Void>> changePostVisibility(
            @PathVariable final long id,
            @Valid @RequestBody final AdminPostVisibilityRequest request) {
        adminPostService.changePostVisibility(id, request.visible());
        final String message = request.visible() ? "게시글이 복구되었습니다." : "게시글이 숨겨졌습니다.";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @Operation(summary = "게시글 고정/고정 해제", description = "고정된 게시글은 목록 조회 시 정렬 기준과 무관하게 최상단에 노출됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "고정 상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> changePostPinned(
            @PathVariable final long id,
            @Valid @RequestBody final AdminPostPinRequest request) {
        adminPostService.changePostPinned(id, request.pinned());
        final String message = request.pinned() ? "게시글이 상단에 고정되었습니다." : "게시글 고정이 해제되었습니다.";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
