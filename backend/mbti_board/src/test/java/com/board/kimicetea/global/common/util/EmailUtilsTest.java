package com.board.kimicetea.global.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailUtilsTest {

    @Nested
    @DisplayName("mask — 이메일 로컬 파트 마스킹")
    class Mask {

        @Test
        @DisplayName("로컬 파트가 3자 이상이면 앞 2자만 노출하고 나머지를 ***로 치환한다")
        void longLocal_masksAfterTwo() {
            assertThat(EmailUtils.mask("abcdef@gmail.com")).isEqualTo("ab***@gmail.com");
        }

        @Test
        @DisplayName("로컬 파트가 정확히 2자이면 전체 노출 후 ***를 붙인다")
        void twoCharLocal_showsAll() {
            assertThat(EmailUtils.mask("ab@gmail.com")).isEqualTo("ab***@gmail.com");
        }

        @Test
        @DisplayName("로컬 파트가 1자이면 그대로 노출 후 ***를 붙인다")
        void oneCharLocal_showsAll() {
            assertThat(EmailUtils.mask("a@gmail.com")).isEqualTo("a***@gmail.com");
        }

        @Test
        @DisplayName("도메인 부분은 마스킹되지 않는다")
        void domain_isNotMasked() {
            assertThat(EmailUtils.mask("test@example.com")).endsWith("@example.com");
        }

        @Test
        @DisplayName("마스킹 결과는 항상 ***@ 패턴을 포함한다")
        void result_alwaysContainsAsterisksBeforeDomain() {
            assertThat(EmailUtils.mask("hello@naver.com")).contains("***@");
        }
    }
}
