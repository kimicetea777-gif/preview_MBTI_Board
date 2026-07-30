package com.board.kimicetea.global.common.exception;

import com.board.kimicetea.global.alert.AlertMarkers;
import com.board.kimicetea.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 정상적인 사용 흐름에서도 자주 발생하는 4xx(글 없음·만료된 링크·중복 이메일 등)는 WARN 대상이 아님 —
     * 보안/운영상 눈에 띄어야 하는 예외만 여기 추가. 나머지는 INFO로 남김
     */
    private static final Set<ErrorCode> NOTABLE_CODES = EnumSet.of(
            ErrorCode.INTERNAL_KEY_INVALID,
            ErrorCode.REUSE_DETECTED,
            ErrorCode.INSUFFICIENT_VISIBLE_QUESTIONS
    );

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(final CustomException e,
            final HttpServletRequest request) {
        final ErrorCode code = e.getErrorCode();
        if (code == ErrorCode.ACCOUNT_LOCKED) {
            log.error(AlertMarkers.ACCOUNT_LOCKED, "Locked account access attempt: {}", request.getRequestURI());
        } else if (code == ErrorCode.RATE_LIMIT_EXCEEDED) {
            final String uri = request.getRequestURI();
            if (uri.contains("login")) {
                log.error(AlertMarkers.BRUTE_FORCE_LOGIN, "Brute force login detected: {}", uri);
            } else if (uri.contains("password-reset")) {
                log.error(AlertMarkers.PASSWORD_RESET_ABUSE, "Password reset abuse detected: {}", uri);
            } else {
                log.error(AlertMarkers.RATE_LIMIT_EXCEEDED, "Rate limit exceeded: {}", uri);
            }
        } else if (NOTABLE_CODES.contains(code)) {
            log.warn("CustomException: {}", e.getMessage());
        } else {
            log.info("CustomException: {}", e.getMessage());
        }
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus().value())
                .body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisConnectionFailure(final RedisConnectionFailureException e) {
        log.error(AlertMarkers.REDIS_CONNECTION_FAIL, "Redis connection failure", e);
        return ResponseEntity.internalServerError().body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    // @Valid DTO객체 @RequestBody 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(final MethodArgumentNotValidException e) {
        final String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    // @RequestParam/@PathVariable 타입 변환 실패 (e.g., ?state=INVALID_VALUE, UUID 파싱 오류)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        final Class<?> requiredType = e.getRequiredType();
        final String typeName = (requiredType != null) ? requiredType.getSimpleName() : "알 수 없는 타입";
        final String message = e.getName() + ": 올바른 " + typeName + " 값이 필요합니다.";
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    // @Validated @RequestParam/@PathVariable 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(final ConstraintViolationException e) {
        final String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    // 필수 @RequestParam 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(final MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getParameterName() + " 파라미터가 누락되었습니다."));
    }

    // 필수 @RequestHeader 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(final MissingRequestHeaderException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getHeaderName() + " 헤더가 누락되었습니다."));
    }

    // JSON 바디 파싱 실패 (문법 오류)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(final HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("잘못된 형식의 요청 데이터(JSON)입니다."));
    }

    // 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(final HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.fail("지원하지 않는 HTTP 메서드 요청입니다."));
    }

    // 동시 요청으로 인한 DB UNIQUE 제약 위반 — 409로 매핑
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(final DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolation on constraint (CONFLICT): {}",
                extractConstraintName(e.getMostSpecificCause().getMessage()));
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("이미 사용 중 입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(final Exception e) {
        log.error(AlertMarkers.UNHANDLED_ERROR, "Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    // ---------------------------------------------Helper--------------------------
    /*
     * 실제 값(이메일 등 PII) 노출 방지 — 제약 조건 이름만 추출
     * duplicate key value violates unique constraint "users_email_key"
     * Detail: Key (email)=(test@test.com) already exists.
     * 
     * -> WARN DataIntegrityViolation on constraint: users_email_key
     */
    private String extractConstraintName(final String message) {
        if (message == null)
            return "unknown";
        final int start = message.indexOf('"');
        if (start == -1)
            return "unknown";

        final int end = message.indexOf('"', start + 1);
        return (end != -1) ? message.substring(start + 1, end) : "unknown";
    }
}
