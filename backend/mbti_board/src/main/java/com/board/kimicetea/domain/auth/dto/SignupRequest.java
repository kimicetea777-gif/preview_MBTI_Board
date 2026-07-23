package com.board.kimicetea.domain.auth.dto;

import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 10, max = 64)
        @Pattern(regexp = ValidationConstants.PASSWORD_REGEXP, message = ValidationConstants.PASSWORD_MESSAGE)
        String password,

        @NotBlank @Size(min = 2, max = 12)
        @Pattern(regexp = ValidationConstants.NICKNAME_REGEXP, message = ValidationConstants.NICKNAME_MESSAGE)
        String nickname,

        /** mbti_metadata.id (예: INTJ, INTP). 미선택 시 "NONE" 전달 */
        @NotBlank
        @Pattern(regexp = ValidationConstants.MBTI_OR_NONE_REGEXP, message = ValidationConstants.MBTI_MESSAGE)
        String mbtiId,

        /** primitive boolean — 필드 누락 시 false로 역직렬화되어 안전하게 거부됨 */
        boolean termsAgreed,
        boolean privacyAgreed,
        boolean age14Agreed
) {}
