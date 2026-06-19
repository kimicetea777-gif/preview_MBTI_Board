package com.board.kimicetea.domain.user.controller;

import com.board.kimicetea.domain.mbti.dto.MbtiTestHistoryResponse;
import com.board.kimicetea.domain.mbti.service.MbtiService;
import com.board.kimicetea.domain.post.PostSortType;
import com.board.kimicetea.domain.post.dto.MyPostPreviewResponse;
import com.board.kimicetea.domain.post.dto.PostSummaryResponse;
import com.board.kimicetea.domain.post.service.PostService;
import com.board.kimicetea.domain.user.dto.ChangePasswordRequest;
import com.board.kimicetea.domain.user.dto.MyProfileResponse;
import com.board.kimicetea.domain.user.dto.UpdatePhoneRequest;
import com.board.kimicetea.domain.user.dto.UpdateProfileRequest;
import com.board.kimicetea.domain.user.dto.UserActivityStatsResponse;
import com.board.kimicetea.domain.user.dto.UserProfileResponse;
import com.board.kimicetea.domain.user.dto.WithdrawalRequest;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.domain.user.service.UserService;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.security.util.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 유저 프로필 API 컨트롤러
 * 주의: /me 엔드포인트는 인증 필수, /{id}는 비인증 접근 허용
 */
@Tag(name = "User", description = "유저 프로필 API")
@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MbtiService mbtiService;
    private final PostService postService;
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
     * 내 게시글 목록 조회 — 유형 필터·정렬·페이지네이션 지원
     */
    @Operation(summary = "내 게시글 목록 조회", description = "로그인한 회원이 작성한 게시글을 조회합니다. type 생략 시 전체 유형을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getMyPosts(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @RequestParam(required = false) final String type,
            @RequestParam(defaultValue = "LATEST") final PostSortType sort,
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) final int size) {

        final Page<PostSummaryResponse> result = postService.getMyPosts(
                userDetails.userId(), type, sort, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 마이페이지 내 게시글 프리뷰 — 최근 5개
     */
    @Operation(summary = "내 게시글 프리뷰 조회", description = "마이페이지용 최근 작성 게시글 5개를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me/posts/preview")
    public ResponseEntity<ApiResponse<List<MyPostPreviewResponse>>> getMyPostsPreview(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(postService.getMyPostsPreview(userDetails.userId())));
    }

    /**
     * 내 활동 통계 조회 — 작성 게시글 수·댓글 수·최근 검사 MBTI
     */
    @Operation(summary = "내 활동 통계 조회", description = "작성한 게시글 수, 댓글 수, 가장 최근 MBTI 검사 결과를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<UserActivityStatsResponse>> getMyActivityStats(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        return ResponseEntity.ok(ApiResponse.ok(userService.getMyActivityStats(userDetails.userId())));
    }

    /**
     * 탈퇴 OTP 발송 — 가입된 이메일로 6자리 인증 코드 전송 (유효 시간 3분)
     */
    @Operation(summary = "탈퇴 인증 코드 발송", description = "가입 이메일로 6자리 OTP를 발송합니다. 3분 이내에 탈퇴 요청에 포함해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 코드 발송 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/me/withdrawal-verify")
    public ResponseEntity<ApiResponse<Void>> sendWithdrawalOtp(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        userService.sendWithdrawalOtp(userDetails.userId());
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 발송되었습니다."));
    }

    /**
     * 회원 탈퇴 — OTP 검증 후 계정 상태를 WITHDRAWN으로 변경
     * 주의: 탈퇴 전 /me/withdrawal-verify 로 OTP를 먼저 발급받아야 함
     */
    @Operation(summary = "회원 탈퇴", description = "OTP 인증 후 계정 상태를 WITHDRAWN으로 변경합니다. 게시글·댓글은 보존됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestHeader("Authorization") final String bearerToken,
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final WithdrawalRequest request,
            final HttpServletResponse response) {

        final String accessToken = bearerToken.substring("Bearer ".length());
        userService.withdraw(userDetails.userId(), accessToken, request.code());
        cookieUtils.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("탈퇴 처리가 완료되었습니다."));
    }

    /**
     * 내 MBTI 검사 이력 조회 — 최신순
     */
    @Operation(summary = "내 MBTI 검사 이력 조회", description = "본인의 MBTI 검사 이력을 최신순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me/mbti/history")
    public ResponseEntity<ApiResponse<List<MbtiTestHistoryResponse>>> getMyMbtiHistory(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(mbtiService.getMyMbtiHistory(userDetails.userId())));
    }

    /**
     * 전화번호 등록/변경 — HMAC 해싱 후 저장, 중복 검증 포함
     */
    @Operation(summary = "전화번호 등록/변경", description = "전화번호를 해싱해 저장합니다. 이미 등록된 번호는 중복 오류를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록/변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 다른 계정에 등록된 전화번호")
    })
    @PatchMapping("/me/phone")
    public ResponseEntity<ApiResponse<Void>> updatePhone(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final UpdatePhoneRequest request) {

        userService.updatePhone(userDetails.userId(), request);
        return ResponseEntity.ok(ApiResponse.ok("전화번호가 등록되었습니다."));
    }

    /**
     * 전화번호 삭제
     */
    @Operation(summary = "전화번호 삭제", description = "등록된 전화번호를 제거합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/me/phone")
    public ResponseEntity<ApiResponse<Void>> removePhone(
            @AuthenticationPrincipal final CustomUserDetails userDetails) {

        userService.removePhone(userDetails.userId());
        return ResponseEntity.ok(ApiResponse.ok("전화번호가 삭제되었습니다."));
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
