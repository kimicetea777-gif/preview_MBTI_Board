package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.LoginResponse;
import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.domain.auth.service.AuthService;
import com.board.kimicetea.domain.user.security.CustomUserDetails;
import com.board.kimicetea.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MBTI_Board 자체 세션(AT/RT) API — 로그인/가입/비밀번호 재설정은 AS(Authorization Server)가 호스팅하므로
 * 여기엔 토큰 재발급·로그아웃만 남는다(connect_manual.md 0번 원칙).
 * 주의: 보안 필터(JwtAuthenticationFilter)를 거친 후 호출됨. logout은 인증된 사용자만 접근 가능
 */
@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth") // 변경 시 'CookieUtils'도 수정 필요
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

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
         * 로그아웃 — AT 블랙리스트 등록 + 해당 기기 RT 삭제 + 클라이언트 Cookie 만료.
         * 소셜 로그인이었다면 응답에 authServerLogoutUrl이 같이 오는데, 프론트는 그 주소로 브라우저를
         * 이동시켜서 AS 세션까지 끝내야 한다 — 안 그러면 다음 로그인 클릭 시 AS 세션이 살아있어 자동으로
         * 같은 계정으로 재로그인돼버린다(다른 계정으로 전환 불가).
         */
        @Operation(summary = "로그아웃", description = "AT를 블랙리스트에 등록하고 RT를 삭제합니다. RT Cookie도 즉시 만료됩니다. "
                        + "authServerLogoutUrl이 응답에 있으면 프론트가 그 주소로 리다이렉트해서 AS 세션도 끝내야 합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<LogoutResponse>> logout(
                        @RequestHeader("Authorization") final String bearerToken,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletResponse response) {

                final String accessToken = bearerToken.substring("Bearer ".length());
                final LogoutResponse logoutResponse = authService.logout(
                                accessToken, userDetails.userId().toString(), userDetails.deviceId(), response);
                return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다.", logoutResponse));
        }

        /**
         * 연동 기기 전체 로그아웃 — 마이페이지에서 다른 기기(공용 PC 등)에 남아있는 세션까지 한번에 정리.
         * /logout과 마찬가지로 authServerLogoutUrl이 오면 프론트가 리다이렉트해서 현재 기기의 AS 세션을 끝내야 함
         */
        @Operation(summary = "전체 기기 로그아웃", description = "현재 계정으로 로그인된 모든 기기의 RT를 삭제합니다. "
                        + "authServerLogoutUrl이 응답에 있으면 프론트가 그 주소로 리다이렉트해서 AS 세션도 끝내야 합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 로그아웃 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/logout/all")
        public ResponseEntity<ApiResponse<LogoutResponse>> logoutAll(
                        @RequestHeader("Authorization") final String bearerToken,
                        @AuthenticationPrincipal final CustomUserDetails userDetails,
                        final HttpServletResponse response) {

                final String accessToken = bearerToken.substring("Bearer ".length());
                final LogoutResponse logoutResponse = authService.logoutAll(
                                accessToken, userDetails.userId().toString(), userDetails.deviceId(), response);
                return ResponseEntity.ok(ApiResponse.ok("모든 기기에서 로그아웃되었습니다.", logoutResponse));
        }
}
