package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

/** 어드민 유저 역할 변경 요청 DTO */
public record AdminUserRoleRequest(
        @NotNull(message = "변경할 역할을 입력해 주세요.")
        UserRole role
) {}
