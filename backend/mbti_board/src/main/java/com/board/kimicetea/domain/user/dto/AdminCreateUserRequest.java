package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 어드민 유저 직접 생성 요청 DTO — 이메일 인증 없이 role·state 직접 지정 */
public record AdminCreateUserRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 10, max = 64)
        @Pattern(regexp = ValidationConstants.PASSWORD_REGEXP, message = ValidationConstants.PASSWORD_MESSAGE)
        String password,

        @NotBlank @Size(min = 2, max = 12)
        @Pattern(regexp = ValidationConstants.NICKNAME_REGEXP, message = ValidationConstants.NICKNAME_MESSAGE)
        String nickname,

        @NotBlank
        @Pattern(regexp = ValidationConstants.MBTI_OR_NONE_REGEXP, message = ValidationConstants.MBTI_MESSAGE)
        String mbtiId,

        @NotNull
        UserRole role,

        @NotNull
        UserState state
) {}
