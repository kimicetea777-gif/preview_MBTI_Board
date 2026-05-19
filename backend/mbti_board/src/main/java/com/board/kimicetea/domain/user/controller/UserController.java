package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.user.dto.ChangePasswordRequest;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.security.util.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 유저 프로필 API 컨트롤러
 * 주의: /me 엔드포인트는 인증 필수, /{id}는 비인증 접근 허용
 */
@Tag(name = "User", description = "유저 프로필 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CookieUtils cookieUtils;

    /**
     * 내 프로필 조회
     */
    @Operation(summary = "내 프로필 조회", description = "JWT 토큰으로 본인의 프로필 전체 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(userDetails.userId())));
    }

    /**
     * 내 프로필 수정 — 닉네임·프로필 이미지·MBTI 선택적 수정
     */
    @Operation(summary = "내 프로필 수정", description = "닉네임, 프로필 이미지 URL, MBTI를 선택적으로 수정합니다. MBTI 변경 시 인증 상태는 SELF로 초기화됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "닉네임 중복")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final UpdateProfileRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(userService.updateMyProfile(userDetails.userId(), request)));
    }

    /**
     * 타인 공개 프로필 조회
     */
    @Operation(summary = "유저 공개 프로필 조회", description = "유저 ID로 공개 프로필을 조회합니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 유저")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable final UUID id) {

        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(id)));
    }

    /**
     * 회원 탈퇴 — 계정 상태를 WITHDRAWN으로 변경
     */
    @Operation(summary = "회원 탈퇴", description = "계정 상태를 WITHDRAWN으로 변경합니다. 게시글·댓글은 보존됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestHeader("Authorization") final String bearerToken,
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            final HttpServletResponse response) {

        final String accessToken = bearerToken.substring("Bearer ".length());
        userService.withdraw(userDetails.userId(), accessToken);
        cookieUtils.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("탈퇴 처리가 완료되었습니다."));
    }

    /**
     * 비밀번호 변경 — 소셜 전용 계정 차단
     */
    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다. 소셜 전용 계정은 사용 불가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 소셜 전용 계정")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final ChangePasswordRequest request) {

        userService.changePassword(userDetails.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
    }
}
