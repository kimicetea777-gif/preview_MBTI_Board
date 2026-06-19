package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.user.dto.AdminCreateUserRequest;
import com.board.kimicetea.domain.user.dto.AdminUpdateUserRequest;
import com.board.kimicetea.domain.user.dto.AdminUserStatsResponse;
import com.board.kimicetea.domain.user.dto.AdminUserSummaryResponse;
import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserSortField;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.domain.user.service.AdminUserService;
import com.board.kimicetea.domain.user.service.AdminUserStatsService;
import org.springframework.data.domain.Sort;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AdminUserStatsService adminUserStatsService;

    @Operation(summary = "유저 통계 조회", description = "상태·역할·MBTI 분포, 가입 추이, 장기 미접속 수 등 유저 현황 통계를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminUserStatsResponse>> getUserStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminUserStatsService.getStats()));
    }

    @Operation(summary = "유저 직접 생성", description = "어드민이 이메일 인증 없이 유저를 직접 생성합니다. role·state를 직접 지정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "유저 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 중복"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createUser(
            @Valid @RequestBody final AdminCreateUserRequest request) {
        adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("유저가 생성되었습니다."));
    }

    @Operation(summary = "유저 목록 조회", description = "이메일·상태·역할 필터, 정렬(createdAt/updatedAt/lastLoginAt), 페이지네이션으로 유저 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponse>>> getUsers(
            @RequestParam(required = false) final String email,
            @RequestParam(required = false) final UserState state,
            @RequestParam(required = false) final UserRole role,
            @RequestParam(defaultValue = "CREATED_AT") final UserSortField sortBy,
            @RequestParam(defaultValue = "DESC") final Sort.Direction sortDir,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.getUsers(email, state, role, sortBy, sortDir,
                        PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100))))));
    }

    @Operation(summary = "유저 통합 수정", description = "닉네임·프로필 사진·역할·상태를 단일 트랜잭션으로 수정합니다. null 필드는 변경하지 않습니다. 자기 자신은 변경 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 자기 자신 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 유저 없음")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @PathVariable final UUID id,
            @Valid @RequestBody final AdminUpdateUserRequest request) {
        adminUserService.updateUser(userDetails.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("유저 정보가 수정되었습니다."));
    }
}
