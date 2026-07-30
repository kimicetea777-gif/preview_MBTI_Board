package com.board.kimicetea.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String email,
        String nickname,
        String role
) {}
