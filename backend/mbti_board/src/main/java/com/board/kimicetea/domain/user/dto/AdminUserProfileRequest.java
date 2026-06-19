package com.board.kimicetea.domain.user.dto;

/**
 * 어드민 유저 프로필 수정 요청 DTO (닉네임 변경·프로필 사진 삭제)
 * 주의: nickname null이면 변경 안 함, clearProfileImage true이면 프로필 사진 삭제
 */
public record AdminUserProfileRequest(
        String nickname,
        Boolean clearProfileImage
) {}
