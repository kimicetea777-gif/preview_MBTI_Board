package com.board.kimicetea.global.internal;

import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.*;

class InternalOnlyAspectTest {

    private static final String VALID_KEY = "valid-internal-key";

    private InternalOnlyAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new InternalOnlyAspect();
        ReflectionTestUtils.setField(aspect, "internalApiKey", VALID_KEY);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindRequest(final String headerValue) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader(InternalApiConstants.INTERNAL_KEY_HEADER, headerValue);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Nested
    @DisplayName("validate — X-Internal-Key 헤더 검증")
    class Validate {

        @Test
        @DisplayName("올바른 키가 있으면 예외를 던지지 않는다")
        void validKey_noException() {
            bindRequest(VALID_KEY);

            assertThatNoException().isThrownBy(() -> aspect.validate());
        }

        @Test
        @DisplayName("헤더가 없으면 INTERNAL_KEY_INVALID 예외를 던진다")
        void missingKey_throwsInvalidKey() {
            bindRequest(null);

            assertThatThrownBy(() -> aspect.validate())
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INTERNAL_KEY_INVALID);
        }

        @Test
        @DisplayName("잘못된 키이면 INTERNAL_KEY_INVALID 예외를 던진다")
        void wrongKey_throwsInvalidKey() {
            bindRequest("wrong-key");

            assertThatThrownBy(() -> aspect.validate())
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INTERNAL_KEY_INVALID);
        }

        @Test
        @DisplayName("빈 문자열 키이면 INTERNAL_KEY_INVALID 예외를 던진다")
        void emptyKey_throwsInvalidKey() {
            bindRequest("");

            assertThatThrownBy(() -> aspect.validate())
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INTERNAL_KEY_INVALID);
        }
    }
}
