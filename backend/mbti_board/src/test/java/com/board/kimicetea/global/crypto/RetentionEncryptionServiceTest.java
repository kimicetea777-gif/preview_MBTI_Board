package com.board.kimicetea.global.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionEncryptionServiceTest {

    private RetentionEncryptionService service;

    @BeforeEach
    void setUp() {
        // salt은 최소 16자 hex 문자열 (8 bytes) 필요
        service = new RetentionEncryptionService("test-password", "0123456789abcdef");
    }

    @Nested
    @DisplayName("encrypt / decrypt — 암호화·복호화 대칭성")
    class EncryptDecrypt {

        @Test
        @DisplayName("암호화한 뒤 복호화하면 원래 평문이 복원된다")
        void encryptThenDecrypt_returnsOriginal() {
            final String plain = "user@example.com";

            assertThat(service.decrypt(service.encrypt(plain))).isEqualTo(plain);
        }

        @Test
        @DisplayName("암호화된 값은 평문과 다르다")
        void encrypted_isDifferentFromPlain() {
            final String plain = "user@example.com";

            assertThat(service.encrypt(plain)).isNotEqualTo(plain);
        }

        @Test
        @DisplayName("같은 평문을 두 번 암호화하면 서로 다른 암호문이 생성된다 (랜덤 IV)")
        void sameInput_producesDifferentCiphertext() {
            final String plain = "user@example.com";

            assertThat(service.encrypt(plain)).isNotEqualTo(service.encrypt(plain));
        }

        @Test
        @DisplayName("null 암호화 시 null을 반환한다")
        void encryptNull_returnsNull() {
            assertThat(service.encrypt(null)).isNull();
        }

        @Test
        @DisplayName("null 복호화 시 null을 반환한다")
        void decryptNull_returnsNull() {
            assertThat(service.decrypt(null)).isNull();
        }
    }
}
