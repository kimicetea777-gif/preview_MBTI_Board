package com.board.kimicetea.global.common.util;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuestPasswordHelper {

    /**
     * 비회원(user == null)이면 rawPassword를 BCrypt 인코딩, 회원이면 null 반환
     * 주의: 게시글·댓글 엔티티의 guestPassword 필드 저장 전에만 호출할 것
     */
    public static String encode(final User user, final String rawPassword, final PasswordEncoder encoder) {
        return (user == null) ? encoder.encode(rawPassword) : null;
    }

    /**
     * 비회원 비밀번호 필수값 검증 — null 또는 빈 값이면 GUEST_PASSWORD_REQUIRED 예외
     * 주의: 비회원 작성/수정/삭제 진입 직후 가장 먼저 호출할 것
     */
    public static void requirePassword(final String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new CustomException(ErrorCode.GUEST_PASSWORD_REQUIRED);
        }
    }
}
