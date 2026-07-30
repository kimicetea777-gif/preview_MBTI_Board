package com.board.kimicetea.global.common.exception;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_RECOVERY_EMAIL(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    RECOVERY_EMAIL_SAME_AS_LOGIN(HttpStatus.BAD_REQUEST, "로그인 이메일과 다른 이메일을 입력해 주세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다."),
    REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다."),
    SOCIAL_ONLY_ACCOUNT(HttpStatus.BAD_REQUEST, "소셜 로그인 전용 계정입니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 Provider입니다."),
    OAUTH2_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "이메일 제공에 동의해야 소셜 로그인이 가능합니다."),
    OAUTH2_SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 가입 요청입니다. 소셜 로그인을 다시 시도해 주세요."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "이용약관·개인정보처리방침 동의 및 만 14세 이상 확인이 모두 필요합니다."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    MBTI_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 MBTI입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 만료되었습니다. 다시 시도해주세요."),
    EMAIL_VERIFY_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 링크입니다. 다시 요청해 주세요."),
    RECOVERY_EMAIL_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 링크입니다. 다시 요청해 주세요."),
    WITHDRAWAL_OTP_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    WITHDRAWAL_OTP_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 다시 요청해 주세요."),
    WITHDRAWAL_OTP_MAX_ATTEMPTS(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해 주세요."),
    FIND_EMAIL_OTP_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    FIND_EMAIL_OTP_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 다시 요청해 주세요."),
    FIND_EMAIL_OTP_MAX_ATTEMPTS(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다. 새로운 인증 코드를 요청해 주세요."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 링크입니다. 다시 요청해 주세요."),
    INTERNAL_KEY_INVALID(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "계정이 잠겨 있습니다."),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "휴면 계정입니다. 고객센터에 문의해 주세요."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴 처리 중인 계정입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "게시글 수정/삭제 권한이 없습니다."),
    POST_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 게시판 유형입니다."),
    POST_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "지원하지 않는 게시판 유형입니다."),
    GUEST_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비회원은 비밀번호를 입력해야 합니다."),
    GUEST_PASSWORD_MISMATCH(HttpStatus.FORBIDDEN, "비밀번호가 일치하지 않습니다."),

    // Debate
    DEBATE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "토론 게시글을 찾을 수 없습니다."),
    DEBATE_ENDS_AT_INVALID(HttpStatus.BAD_REQUEST, "마감일은 현재 시각보다 이후여야 합니다."),
    DEBATE_STANCE_CLOSED(HttpStatus.BAD_REQUEST, "마감된 토론에는 의견을 선택할 수 없습니다."),
    DEBATE_STANCE_INVALID(HttpStatus.BAD_REQUEST, "찬반 입장은 1(찬성) 또는 -1(반대)이어야 합니다."),

    // Vote
    VOTE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 게시글을 찾을 수 없습니다."),
    VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 선택지를 찾을 수 없습니다."),
    VOTE_OPTION_NOT_BELONGS(HttpStatus.BAD_REQUEST, "선택지가 해당 투표 게시글에 속하지 않습니다."),
    VOTE_ENDED(HttpStatus.BAD_REQUEST, "이미 마감된 투표입니다."),
    VOTE_OPTIONS_REQUIRED(HttpStatus.BAD_REQUEST, "투표 게시글은 최소 2개 이상의 선택지가 필요합니다."),
    VOTE_OPTIONS_EXCEEDED(HttpStatus.BAD_REQUEST, "투표 선택지는 최대 10개까지 등록할 수 있습니다."),
    VOTE_MBTI_DUPLICATE(HttpStatus.BAD_REQUEST, "같은 투표에 동일한 MBTI 선택지를 중복 등록할 수 없습니다."),
    VOTE_MBTI_MODE_MIXED(HttpStatus.BAD_REQUEST, "MBTI 선택지와 일반 선택지를 혼합할 수 없습니다."),
    VOTE_OPTION_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "자유 투표 선택지는 텍스트가 필수입니다."),
    VOTE_OPTION_TEXT_DUPLICATE(HttpStatus.BAD_REQUEST, "같은 투표에 동일한 텍스트 선택지를 중복 등록할 수 없습니다."),
    VOTE_ENDS_AT_INVALID(HttpStatus.BAD_REQUEST, "마감일은 현재 시각보다 이후여야 합니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 댓글을 달 수 없습니다."),
    DEBATE_STANCE_REQUIRED(HttpStatus.BAD_REQUEST, "토론 게시글 댓글은 찬반 입장(1 또는 -1)을 선택해야 합니다."),
    DEBATE_STANCE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "일반·투표 게시글 댓글에는 찬반 입장을 설정할 수 없습니다."),
    INVALID_REACTION_TYPE(HttpStatus.BAD_REQUEST, "리액션 타입은 1(좋아요) 또는 -1(싫어요)이어야 합니다."),

    // Notice
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공지사항입니다."),

    // Admin
    ADMIN_CANNOT_MODIFY_SELF(HttpStatus.BAD_REQUEST, "자기 자신의 상태/역할은 변경할 수 없습니다."),
    ADMIN_INVALID_CREATE_STATE(HttpStatus.BAD_REQUEST, "WITHDRAWN 상태로는 유저를 생성할 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문항입니다."),
    INSUFFICIENT_VISIBLE_QUESTIONS(HttpStatus.SERVICE_UNAVAILABLE, "출제 가능한 문항이 부족합니다. 관리자에게 문의하세요."),

    // Image
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. (jpg·png·webp·bmp만 허용)"),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "이미지 크기는 10MB를 초과할 수 없습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
    IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 3개까지 첨부할 수 있습니다."),

    // Common
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatusCode httpStatus;
    private final String message;
}
