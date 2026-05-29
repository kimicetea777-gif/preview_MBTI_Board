package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.EmailVerifyConfirmRequest;
import com.board.kimicetea.domain.auth.dto.EmailVerifySendRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailRequest;
import com.board.kimicetea.domain.auth.dto.FindEmailResponse;
import com.board.kimicetea.domain.auth.dto.LoginRequest;
import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.PasswordResetConfirmRequest;
import com.board.kimicetea.domain.auth.dto.PasswordResetRequestBody;
import com.board.kimicetea.domain.auth.dto.SignupRequest;
import com.board.kimicetea.domain.auth.service.AuthService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 인증 API 컨트롤러 (회원가입·로그인·토큰 재발급·로그아웃)
 * 주의: 보안 필터(JwtAuthenticationFilter)를 거친 후 호출됨. logout은 인증된 사용자만 접근 가능
 */
@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth") // 변경 시 'CookieUtils'도 수정 필요
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        /**
         * 닉네임으로 이메일 찾기 — WITHDRAWN 계정은 404 반환
         */
        @Operation(summary = "이메일 찾기", description = "닉네임으로 마스킹된 이메일을 조회합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 조회 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 닉네임의 사용자 없음")
        })
        @PostMapping("/find-email")
        public ResponseEntity<ApiResponse<FindEmailResponse>> findEmail(
                        @Valid @RequestBody final FindEmailRequest request) {
                return ResponseEntity.ok(ApiResponse.ok(authService.findEmail(request)));
        }

        /**
         * 비밀번호 재설정 이메일 발송 — 이메일 열거 방지를 위해 항상 동일 응답 반환
         */
        @Operation(summary = "비밀번호 재설정 이메일 발송", description = "이메일로 비밀번호 재설정 링크를 발송합니다. 이메일 존재 여부와 무관하게 항상 200을 반환합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 처리 완료")
        })
        @PostMapping("/password-reset/request")
        public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
                        @Valid @RequestBody final PasswordResetRequestBody request) {
                authService.requestPasswordReset(request);
                return ResponseEntity.ok(ApiResponse.ok("이메일이 발송되었습니다."));
        }

        /**
         * 비밀번호 재설정 확인 — 토큰 검증 후 비밀번호 변경 + 전 기기 강제 로그아웃
         */
        @Operation(summary = "비밀번호 재설정 확인", description = "재설정 토큰과 새 비밀번호로 비밀번호를 변경합니다. 전 기기 강제 로그아웃됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 만료 또는 무효")
        })
        @PostMapping("/password-reset/confirm")
        public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
                        @Valid @RequestBody final PasswordResetConfirmRequest request) {
                authService.confirmPasswordReset(request);
                return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
        }

        /**
         * 이메일 인증 링크 발송 — 프론트 /signup/verify?token={uuid} 로 연결되는 링크 발송
         */
        @Operation(summary = "이메일 인증 발송", description = "입력한 이메일로 인증 링크를 발송합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발송 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이메일 형식 오류")
        })
        @PostMapping("/email-verify/send")
        public ResponseEntity<ApiResponse<Void>> sendEmailVerification(
                        @Valid @RequestBody final EmailVerifySendRequest request) {
                authService.sendEmailVerification(request);
                return ResponseEntity.ok(ApiResponse.ok("인증 이메일이 발송되었습니다."));
        }

        /**
         * 이메일 인증 토큰 확인 — 프론트가 링크의 token 파라미터를 이 API로 전달
         */
        @Operation(summary = "이메일 인증 확인", description = "이메일 링크의 token을 검증하고 인증 완료 상태를 저장합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 만료 또는 유효하지 않음")
        })
        @PostMapping("/email-verify/confirm")
        public ResponseEntity<ApiResponse<String>> confirmEmailVerification(
                        @Valid @RequestBody final EmailVerifyConfirmRequest request) {
                final String verifiedEmail = authService.confirmEmailVerification(request);
                return ResponseEntity.ok(ApiResponse.ok("이메일 인증이 완료되었습니다.", verifiedEmail));
        }

        /**
         * 로컬 회원가입
         */
        @Operation(summary = "회원가입", description = "이메일·비밀번호·닉네임·MBTI로 로컬 계정을 생성합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복")
        })
        @PostMapping("/signup")
        public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody final SignupRequest request) {
                authService.signup(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("회원가입이 완료되었습니다."));
        }

        /**
         * 로컬 로그인 — Access Token(body) + Refresh Token(HttpOnly Cookie) 발급
         */
        @Operation(summary = "로그인", description = "이메일·비밀번호로 로그인합니다. AT는 body, RT는 HttpOnly Cookie로 반환됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
        })
        @PostMapping("/login")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody final LoginRequest request,
                        final HttpServletResponse response) {

                return ResponseEntity.ok(ApiResponse.ok(authService.login(request, response)));
        }

        /**
         * Access Token 재발급 — RT Rotation + Reuse Detection 포함
         */
        @Operation(summary = "토큰 재발급", description = "RT Cookie를 이용해 AT·RT를 모두 재발급합니다 (RT Rotation).")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "RT 만료·불일치 또는 재사용 감지")
        })
        @PostMapping("/reissue")
        public ResponseEntity<ApiResponse<LoginResponse>> reissue(
                        final HttpServletRequest request,
                        final HttpServletResponse response) {

                return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request, response)));
        }

        /**
         * 로그아웃 — AT 블랙리스트 등록 + 해당 기기 RT 삭제 + 클라이언트 Cookie 만료
         */
        @Operation(summary = "로그아웃", description = "AT를 블랙리스트에 등록하고 RT를 삭제합니다. RT Cookie도 즉시 만료됩니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @RequestHeader("Authorization") final String bearerToken,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletResponse response) {

                final String accessToken = bearerToken.substring("Bearer ".length());
                authService.logout(accessToken, userDetails.userId().toString(), userDetails.deviceId(), response);
                return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다."));
        }
}
