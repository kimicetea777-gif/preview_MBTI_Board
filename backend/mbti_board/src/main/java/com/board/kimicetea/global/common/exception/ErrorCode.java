package com.board.kimicetea.global.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다."),
    REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다."),
    SOCIAL_ONLY_ACCOUNT(HttpStatus.BAD_REQUEST, "소셜 로그인 전용 계정입니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 Provider입니다."),
    OAUTH2_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 제공에 동의해야 소셜 로그인이 가능합니다."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token 쿠키가 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    MBTI_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 MBTI입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    EMAIL_VERIFY_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 링크입니다. 다시 요청해 주세요."),
    WITHDRAWAL_OTP_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    WITHDRAWAL_OTP_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 다시 요청해 주세요."),
    WITHDRAWAL_OTP_MAX_ATTEMPTS(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해 주세요."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겨 있습니다."),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "휴면 계정입니다. 고객센터에 문의해 주세요."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴 처리 중인 계정입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "게시글 수정/삭제 권한이 없습니다."),
    POST_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 게시판 유형입니다."),
    GUEST_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비회원은 비밀번호를 입력해야 합니다."),
    GUEST_PASSWORD_MISMATCH(HttpStatus.FORBIDDEN, "비밀번호가 일치하지 않습니다."),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
