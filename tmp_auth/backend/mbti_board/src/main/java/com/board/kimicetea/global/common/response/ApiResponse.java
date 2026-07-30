package com.board.kimicetea.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 * 모든 API 응답을 감싸는 공통 포맷
 * 주의: data가 null인 경우 직렬화에서 제외됨 (JsonInclude.NON_NULL)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    /* ------------------------------OK---------------------------------- */
    public static <T> ApiResponse<T> ok(final T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(final String message, final T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(final String message) {
        return new ApiResponse<>(true, message, null);
    }

    /* -----------------------------FAIL--------------------------------- */
    public static ApiResponse<Void> fail(final String message) {
        return new ApiResponse<>(false, message, null);
    }
}
