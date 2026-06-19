package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.enums.UserRole;
import com.board.kimicetea.domain.user.enums.UserState;
import com.board.kimicetea.global.common.ValidationConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 어드민 유저 통합 수정 요청 DTO — null 필드는 변경하지 않음
 * 주의: nickname·role·state 중 변경할 것만 포함해서 전송
 */
public record AdminUpdateUserRequest(

        @Size(min = 2, max = 12)
        @Pattern(regexp = ValidationConstants.NICKNAME_REGEXP, message = ValidationConstants.NICKNAME_MESSAGE)
        String nickname,

        Boolean clearProfileImage,

        UserRole role,

        UserState state
) {}
