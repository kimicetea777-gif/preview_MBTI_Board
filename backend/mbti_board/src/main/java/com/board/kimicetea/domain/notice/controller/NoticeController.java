package com.board.kimicetea.domain.notice.controller;

import com.board.kimicetea.domain.notice.dto.NoticeResponse;
import com.board.kimicetea.domain.notice.service.NoticeService;
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
 * 공개 공지사항 API 컨트롤러 — 인증 불필요
 * 주의: SecurityConfig에서 GET /api/notices/active permitAll 처리
 */
@Tag(name = "Notice", description = "공지사항 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "활성 공지사항 목록 조회", description = "현재 시각 기준 노출 가능한 공지를 priority 내림차순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> getActiveNotices() {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getActiveNotices()));
    }
}
