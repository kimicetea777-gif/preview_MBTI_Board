package com.board.kimicetea.global.image.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 HTTP로 confirm 응답 해석을 확인한다 — JDK 내장 HttpServer로 Image Server 응답을 흉내 낸다 */
class ImageServerClientTest {

    private HttpServer server;
    private ImageServerClient client;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicReference<String> lastApiKey = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";
    private volatile long responseDelayMillis = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // 응답을 늦추는 테스트에서 핸들러가 잠들어 있어도 서버 종료가 막히지 않게 별도 스레드에서 처리
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/images/confirm", exchange -> {
            requestCount.incrementAndGet();
            lastApiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            exchange.getRequestBody().readAllBytes();
            if (responseDelayMillis > 0) {
                try {
                    Thread.sleep(responseDelayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            final byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        client = new ImageServerClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-key");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("요청한 id가 전부 confirmed에 있으면 빈 목록을 반환한다")
    void allConfirmed() {
        responseBody = "{\"confirmed\":[\"a\",\"b\"]}";

        assertThat(client.confirm(List.of("a", "b"))).isEmpty();
        assertThat(lastApiKey.get()).isEqualTo("test-key");
    }

    @Test
    @DisplayName("200이어도 confirmed에서 빠진 id(만료·삭제)는 미확정으로 반환한다")
    void partiallyConfirmed() {
        responseBody = "{\"confirmed\":[\"a\"]}";

        assertThat(client.confirm(List.of("a", "expired", "missing"))).containsExactly("expired", "missing");
    }

    @Test
    @DisplayName("200인데 본문을 읽을 수 없으면 '전부 누락'으로 오판하지 않고 예외로 알린다 — 워커가 재시도한다")
    void unreadableBodyThrows() {
        responseBody = "not-json";

        assertThatThrownBy(() -> client.confirm(List.of("a", "b")))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(RestClientResponseException.class);
    }

    @Test
    @DisplayName("200인데 confirmed 목록이 없으면 예외로 알린다")
    void missingConfirmedFieldThrows() {
        responseBody = "{\"unexpected\":true}";

        assertThatThrownBy(() -> client.confirm(List.of("a")))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("HTTP 오류는 응답 코드를 담은 예외로 전파된다 — 워커가 코드로 재시도 여부를 판단한다")
    void serverErrorPropagates() {
        responseStatus = 500;

        assertThatThrownBy(() -> client.confirm(List.of("a")))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    @DisplayName("응답이 타임아웃보다 늦으면 기다리지 않고 ResourceAccessException으로 실패한다")
    void slowResponseTimesOut() {
        responseDelayMillis = 2_000;
        responseBody = "{\"confirmed\":[\"a\"]}";
        final ImageServerClient shortTimeoutClient = new ImageServerClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-key",
                Duration.ofSeconds(1), Duration.ofMillis(300));

        final long startedAt = System.nanoTime();
        assertThatThrownBy(() -> shortTimeoutClient.confirm(List.of("a")))
                .isInstanceOf(ResourceAccessException.class);
        final long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(elapsedMillis).isLessThan(1_500);
    }

    @Test
    @DisplayName("운영 기본 타임아웃은 연결 2초, 응답 5초다")
    void defaultTimeouts() {
        assertThat(ImageServerClient.CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(2));
        assertThat(ImageServerClient.READ_TIMEOUT).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("확정할 id가 없으면 요청을 보내지 않는다")
    void emptyIdsSkipsRequest() {
        assertThat(client.confirm(List.of())).isEmpty();
        assertThat(requestCount.get()).isZero();
    }
}
