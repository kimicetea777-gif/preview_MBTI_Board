package com.board.kimicetea.global.security.handler;

import com.board.kimicetea.global.common.response.ApiResponse;
import com.board.kimicetea.global.security.session.SessionAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 미인증 요청(401) 시 HTML 리다이렉트 대신 JSON 응답 반환.
 * SessionAuthenticationFilter가 남겨둔 원인별 어트리뷰트로 메시지를 구분한다(memo2.md 5절:
 * AS가 명시적으로 거부한 것과 Redis/AS 일시 장애를 프론트에 다르게 안내할 필요는 없지만,
 * 사람이 읽는 메시지는 원인이 드러나야 디버깅이 쉬움 — 실패 처리(세션 삭제 여부) 자체는 필터에서 이미 갈림).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException) throws IOException {
        final String sessionError = (String) request.getAttribute(SessionAuthenticationFilter.SESSION_ERROR_ATTRIBUTE);
        final String message = switch (sessionError == null ? "" : sessionError) {
            case "SESSION_INVALID" -> "세션이 만료되었습니다. 다시 로그인해 주세요.";
            case "ACCOUNT_BLOCKED" -> "접근이 제한된 계정입니다.";
            case "TEMPORARILY_UNAVAILABLE" -> "일시적인 오류로 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "인증이 필요합니다.";
        };

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
    }
}
