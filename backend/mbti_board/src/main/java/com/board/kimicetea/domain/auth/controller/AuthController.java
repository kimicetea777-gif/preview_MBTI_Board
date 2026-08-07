package com.board.kimicetea.domain.auth.controller;

import com.board.kimicetea.domain.auth.dto.LogoutResponse;
import com.board.kimicetea.domain.auth.service.AuthService;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.security.session.SessionCookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 세션 로그아웃 API — 로그인/가입/비밀번호 재설정은 AS(Authorization Server)가 호스팅하므로
 * 여기엔 로그아웃만 남는다(connect_manual.md 0번 원칙). 재발급(reissue) 엔드포인트는 더 이상 필요
 * 없음 — 브라우저가 들고 있는 토큰이 없고, AS access token 갱신은 매 요청마다 서버가 조용히 처리한다.
 * 주의: 보안 필터(SessionAuthenticationFilter)를 거친 후 호출됨.
 */
@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        /**
         * 로그아웃 — 세션 삭제 + 세션 쿠키 즉시 만료.
         * 소셜 로그인이었다면 응답에 authServerLogoutUrl이 같이 오는데, 프론트는 그 주소로 브라우저를
         * 이동시켜서 AS 세션까지 끝내야 한다 — 안 그러면 다음 로그인 클릭 시 AS 세션이 살아있어 자동으로
         * 같은 계정으로 재로그인돼버린다(다른 계정으로 전환 불가).
         */
        @Operation(summary = "로그아웃", description = "서버 세션을 삭제합니다. 세션 쿠키도 즉시 만료됩니다. "
                        + "authServerLogoutUrl이 응답에 있으면 프론트가 그 주소로 리다이렉트해서 AS 세션도 끝내야 합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/logout")
        public ResponseEntity<ApiResponse<LogoutResponse>> logout(
                        final HttpServletRequest request,
                        final HttpServletResponse response) {

                final String sessionId = extractSessionIdOrThrow(request);
                final LogoutResponse logoutResponse = authService.logout(sessionId, response);
                return ResponseEntity.ok(ApiResponse.ok("로그아웃되었습니다.", logoutResponse));
        }

        /**
         * 연동 기기 전체 로그아웃 — 마이페이지에서 다른 기기(공용 PC 등)에 남아있는 세션까지 한번에 정리.
         * /logout과 마찬가지로 authServerLogoutUrl이 오면 프론트가 리다이렉트해서 현재 기기의 AS 세션을 끝내야 함
         */
        @Operation(summary = "전체 기기 로그아웃", description = "현재 계정으로 로그인된 모든 기기의 세션을 삭제합니다. "
                        + "authServerLogoutUrl이 응답에 있으면 프론트가 그 주소로 리다이렉트해서 AS 세션도 끝내야 합니다.")
        @ApiResponses({
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 로그아웃 성공"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
        })
        @PostMapping("/logout/all")
        public ResponseEntity<ApiResponse<LogoutResponse>> logoutAll(
                        final HttpServletRequest request,
                        final HttpServletResponse response) {

                final String sessionId = extractSessionIdOrThrow(request);
                final LogoutResponse logoutResponse = authService.logoutAll(sessionId, response);
                return ResponseEntity.ok(ApiResponse.ok("모든 기기에서 로그아웃되었습니다.", logoutResponse));
        }

        private String extractSessionIdOrThrow(final HttpServletRequest request) {
                return SessionCookieUtils.extractSessionId(request)
                                .orElseThrow(() -> new CustomException(ErrorCode.MISSING_REFRESH_TOKEN));
        }
}
