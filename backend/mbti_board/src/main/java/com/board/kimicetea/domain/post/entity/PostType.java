package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;

import java.util.Optional;

public enum PostType {
    FREE, VOTE, DEBATE;

    /** 대소문자 구분 없이 변환. 유효하지 않은 값이면 POST_TYPE_NOT_FOUND 예외 */
    public static PostType from(final String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.POST_TYPE_NOT_FOUND);
        }
    }

    /** null·공백이면 empty, 그 외에는 from()과 동일하게 변환(유효하지 않으면 예외) */
    public static Optional<PostType> fromNullable(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(from(value));
    }
}
