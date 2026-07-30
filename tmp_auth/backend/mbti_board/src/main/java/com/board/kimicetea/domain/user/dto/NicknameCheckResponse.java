package com.board.kimicetea.domain.user.dto;

/**
 * 닉네임 실시간 중복 체크 응답 DTO (GET /api/users/check-nickname)
 */
public record NicknameCheckResponse(
        boolean available
) {
}
