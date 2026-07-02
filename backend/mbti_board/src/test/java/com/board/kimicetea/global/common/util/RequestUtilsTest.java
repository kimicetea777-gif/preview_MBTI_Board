package com.board.kimicetea.global.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilsTest {

    @Nested
    @DisplayName("extractClientIp — Cloudflare 환경 클라이언트 IP 추출")
    class ExtractClientIp {

        @Test
        @DisplayName("CF-Connecting-IP 헤더가 있으면 해당 값을 반환한다")
        void cfConnectingIp_returned() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("CF-Connecting-IP", "1.2.3.4");
            request.addHeader("X-Forwarded-For", "9.9.9.9");

            assertThat(RequestUtils.extractClientIp(request)).isEqualTo("1.2.3.4");
        }

        @Test
        @DisplayName("CF-Connecting-IP 없고 X-Forwarded-For 있으면 첫 번째 IP를 반환한다")
        void xForwardedFor_firstIpReturned() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", "2.3.4.5, 10.0.0.1, 172.16.0.1");

            assertThat(RequestUtils.extractClientIp(request)).isEqualTo("2.3.4.5");
        }

        @Test
        @DisplayName("두 헤더 모두 없으면 RemoteAddr를 반환한다")
        void noHeaders_remoteAddrReturned() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("3.4.5.6");

            assertThat(RequestUtils.extractClientIp(request)).isEqualTo("3.4.5.6");
        }

        @Test
        @DisplayName("CF-Connecting-IP 헤더 값의 앞뒤 공백을 제거한다")
        void cfConnectingIp_stripped() {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("CF-Connecting-IP", "  1.2.3.4  ");

            assertThat(RequestUtils.extractClientIp(request)).isEqualTo("1.2.3.4");
        }
    }

    @Nested
    @DisplayName("hashIp — IP SHA-256 해싱")
    class HashIp {

        @Test
        @DisplayName("동일한 salt와 ip는 항상 같은 해시를 반환한다")
        void sameInput_sameHash() {
            final String hash1 = RequestUtils.hashIp("salt", "1.2.3.4");
            final String hash2 = RequestUtils.hashIp("salt", "1.2.3.4");

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("salt가 다르면 같은 IP라도 다른 해시를 반환한다")
        void differentSalt_differentHash() {
            final String hash1 = RequestUtils.hashIp("saltA", "1.2.3.4");
            final String hash2 = RequestUtils.hashIp("saltB", "1.2.3.4");

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("반환값은 SHA-256 Hex 문자열(64자)이다")
        void result_is64CharHex() {
            final String hash = RequestUtils.hashIp("salt", "1.2.3.4");

            assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("ip가 다르면 같은 salt라도 다른 해시를 반환한다")
        void differentIp_differentHash() {
            final String hash1 = RequestUtils.hashIp("salt", "1.2.3.4");
            final String hash2 = RequestUtils.hashIp("salt", "5.6.7.8");

            assertThat(hash1).isNotEqualTo(hash2);
        }
    }
}
