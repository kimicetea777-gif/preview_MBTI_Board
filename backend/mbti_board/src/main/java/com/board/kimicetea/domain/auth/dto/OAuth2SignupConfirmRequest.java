package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 신규 가입 약관 동의 완료 요청 DTO
 * 주의: termsAgreed/privacyAgreed는 primitive boolean — JSON에 필드 자체가 없으면 false로 역직렬화되어 안전하게 거부됨
 */
public record OAuth2SignupConfirmRequest(
        @NotBlank(message = "가입 요청 토큰이 필요합니다.")
        String signupToken,

        boolean termsAgreed,
        boolean privacyAgreed
) {}
