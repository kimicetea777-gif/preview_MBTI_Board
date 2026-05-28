package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.user.dto.AdminUserRoleRequest;
import com.board.kimicetea.domain.user.dto.AdminUserStateRequest;
import com.board.kimicetea.domain.user.dto.AdminUserSummaryResponse;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.service.AdminUserService;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 어드민 유저 관리 API 컨트롤러
 * 주의: SecurityConfig에서 /api/admin/** → ROLE_ADMIN 보호 필수
 */
@Tag(name = "Admin - User", description = "어드민 유저 관리 API")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 목록 조회", description = "이메일·상태·역할 필터와 페이지네이션으로 유저 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponse>>> getUsers(
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) final UserState state,
            @RequestParam(required = false) final UserRole role,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.getUsers(email, state, role, PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100))))));
    }

    @Operation(summary = "유저 상태 변경", description = "특정 유저의 계정 상태를 변경합니다. 자기 자신은 변경 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 유저 없음")
    })
    @PatchMapping("/{id}/state")
    public ResponseEntity<ApiResponse<Void>> changeUserState(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @PathVariable final UUID id,
            @Valid @RequestBody final AdminUserStateRequest request) {
        adminUserService.changeUserState(userDetails.userId(), id, request.state());
        return ResponseEntity.ok(ApiResponse.ok("유저 상태가 변경되었습니다."));
    }

    @Operation(summary = "유저 역할 변경", description = "특정 유저의 권한 역할을 변경합니다. 자기 자신은 변경 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 유저 없음")
    })
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @PathVariable final UUID id,
            @Valid @RequestBody final AdminUserRoleRequest request) {
        adminUserService.changeUserRole(userDetails.userId(), id, request.role());
        return ResponseEntity.ok(ApiResponse.ok("유저 역할이 변경되었습니다."));
    }
}
