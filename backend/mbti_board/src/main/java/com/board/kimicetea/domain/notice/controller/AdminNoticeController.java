package com.board.kimicetea.domain.notice.controller;

import com.board.kimicetea.domain.notice.dto.AdminNoticeResponse;
import com.board.kimicetea.domain.notice.dto.NoticeCreateRequest;
import com.board.kimicetea.domain.notice.dto.NoticeUpdateRequest;
import com.board.kimicetea.domain.notice.service.AdminNoticeService;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 어드민 공지사항 관리 API 컨트롤러
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 필수
 */
@Tag(name = "Admin - Notice", description = "어드민 공지사항 관리 API")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @Operation(summary = "공지사항 전체 목록 조회", description = "비활성 공지 포함 전체 목록을 priority 내림차순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminNoticeResponse>>> getAllNotices() {
        return ResponseEntity.ok(ApiResponse.ok(adminNoticeService.getAllNotices()));
    }

    @Operation(summary = "공지사항 생성", description = "새 공지사항을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> createNotice(
            @Valid @RequestBody final NoticeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminNoticeService.createNotice(request)));
    }

    @Operation(summary = "공지사항 수정", description = "공지 제목·내용·활성 여부·기간을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> updateNotice(
            @PathVariable final long id,
            @Valid @RequestBody final NoticeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminNoticeService.updateNotice(id, request)));
    }

    @Operation(summary = "공지사항 삭제", description = "공지사항을 영구 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable final long id) {
        adminNoticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.ok("공지사항이 삭제되었습니다."));
    }
}
