package com.board.kimicetea.global.common.util;

import com.board.kimicetea.domain.user.entity.User;
import com.board.kimicetea.global.common.exception.CustomException;
import com.board.kimicetea.global.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GuestPasswordHelperTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Nested
    @DisplayName("encode — 비회원/회원 여부에 따른 비밀번호 인코딩")
    class Encode {

        @Test
        @DisplayName("user가 null이면 rawPassword를 BCrypt로 인코딩한 값을 반환한다")
        void nullUser_encodesPassword() {
            final String raw = "guest1234";

            final String encoded = GuestPasswordHelper.encode(null, raw, encoder);

            assertThat(encoded).isNotNull();
            assertThat(encoder.matches(raw, encoded)).isTrue();
        }

        @Test
        @DisplayName("user가 존재하면 null을 반환한다")
        void existingUser_returnsNull() {
            final User user = mock(User.class);

            final String encoded = GuestPasswordHelper.encode(user, "guest1234", encoder);

            assertThat(encoded).isNull();
        }
    }

    @Nested
    @DisplayName("requirePassword — 비회원 비밀번호 필수값 검증")
    class RequirePassword {

        @Test
        @DisplayName("rawPassword가 null이면 GUEST_PASSWORD_REQUIRED 예외를 던진다")
        void null_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requirePassword(null))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GUEST_PASSWORD_REQUIRED);
        }

        @Test
        @DisplayName("rawPassword가 빈 문자열이면 GUEST_PASSWORD_REQUIRED 예외를 던진다")
        void empty_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requirePassword(""))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GUEST_PASSWORD_REQUIRED);
        }

        @Test
        @DisplayName("rawPassword가 공백만 있으면 GUEST_PASSWORD_REQUIRED 예외를 던진다")
        void blank_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requirePassword("   "))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.GUEST_PASSWORD_REQUIRED);
        }

        @Test
        @DisplayName("유효한 비밀번호는 예외를 던지지 않는다")
        void valid_noException() {
            assertThatNoException().isThrownBy(() -> GuestPasswordHelper.requirePassword("guest1234"));
        }
    }

    @Nested
    @DisplayName("requireTermsAgreement — 비회원 이용약관·개인정보처리방침·만 14세 확인 검증")
    class RequireTermsAgreement {

        @Test
        @DisplayName("termsAgreed가 false이면 TERMS_NOT_AGREED 예외를 던진다")
        void termsNotAgreed_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requireTermsAgreement(false, true, true))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("privacyAgreed가 false이면 TERMS_NOT_AGREED 예외를 던진다")
        void privacyNotAgreed_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requireTermsAgreement(true, false, true))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("age14Agreed가 false이면 TERMS_NOT_AGREED 예외를 던진다")
        void age14NotAgreed_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requireTermsAgreement(true, true, false))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("셋 다 false이면 TERMS_NOT_AGREED 예외를 던진다")
        void allFalse_throwsException() {
            assertThatThrownBy(() -> GuestPasswordHelper.requireTermsAgreement(false, false, false))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TERMS_NOT_AGREED);
        }

        @Test
        @DisplayName("셋 다 true이면 예외를 던지지 않는다")
        void allTrue_noException() {
            assertThatNoException().isThrownBy(() -> GuestPasswordHelper.requireTermsAgreement(true, true, true));
        }
    }
}
