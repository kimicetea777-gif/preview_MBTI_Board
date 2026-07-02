package com.board.kimicetea.domain.post.entity;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;

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
}
