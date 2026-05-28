package com.board.kimicetea.domain.user.dto;

import com.board.kimicetea.domain.user.enums.UserState;
import jakarta.validation.constraints.NotNull;

/** 어드민 유저 상태 변경 요청 DTO */
public record AdminUserStateRequest(
        @NotNull(message = "변경할 상태를 입력해 주세요.")
        UserState state
) {}
