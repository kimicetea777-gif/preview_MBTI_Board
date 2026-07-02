package com.board.kimicetea.global.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneHashServiceTest {

    private PhoneHashService phoneHashService;

    @BeforeEach
    void setUp() {
        phoneHashService = new PhoneHashService("test-hmac-secret");
    }

    @Nested
    @DisplayName("hash — 전화번호 HMAC-SHA256 해싱")
    class Hash {

        @Test
        @DisplayName("같은 전화번호는 항상 같은 해시를 반환한다 (결정성)")
        void samePhone_returnsSameHash() {
            final String hash1 = phoneHashService.hash("01012345678");
            final String hash2 = phoneHashService.hash("01012345678");

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("다른 전화번호는 다른 해시를 반환한다")
        void differentPhone_returnsDifferentHash() {
            assertThat(phoneHashService.hash("01012345678"))
                    .isNotEqualTo(phoneHashService.hash("01087654321"));
        }

        @Test
        @DisplayName("해시 결과는 64자리 소문자 hex 문자열이다")
        void result_is64CharHex() {
            final String hash = phoneHashService.hash("01012345678");

            assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("하이픈이 포함된 번호와 숫자만 있는 번호의 해시가 같다 (정규화)")
        void hyphen_normalizedBeforeHashing() {
            assertThat(phoneHashService.hash("010-1234-5678"))
                    .isEqualTo(phoneHashService.hash("01012345678"));
        }

        @Test
        @DisplayName("공백이 포함된 번호도 정규화하여 동일한 해시를 반환한다")
        void spaces_normalizedBeforeHashing() {
            assertThat(phoneHashService.hash("010 1234 5678"))
                    .isEqualTo(phoneHashService.hash("01012345678"));
        }
    }
}
