package com.board.kimicetea.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 64)
        String password,

        @NotBlank @Size(min = 2, max = 50)
        String nickname,

        /** mbti_metadata.id (예: INTJ, INTP). 미선택 시 "NONE" 전달 */
        @NotBlank
        String mbtiId
) {}
