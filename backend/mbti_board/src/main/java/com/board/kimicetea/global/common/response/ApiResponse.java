package com.board.kimicetea.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 포맷
 * 주의: data가 null인 경우 직렬화에서 제외됨 (JsonInclude.NON_NULL)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(final boolean success, final String message, final T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(final T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(final String message, final T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(final String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Void> fail(final String message) {
        return new ApiResponse<>(false, message, null);
    }
}
