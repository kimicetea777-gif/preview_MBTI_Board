package com.board.kimicetea.global.image.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Image Server(공용 이미지 업로드 서버)의 confirm/release만 호출하는 얇은 클라이언트.
 * 업로드는 Next.js가 Image Server를 직접 호출하고, 확정·정리는 PostImageSyncService(워커)만 이 클라이언트로 한다.
 */
@Component
public class ImageServerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 연결·응답 대기 상한. 워커는 트랜잭션 없이 호출하지만, 워커 한 번의 실행이 Image Server 장애로 무한정
     * 늘어지지 않게 막는다. 상한을 넘기면 ResourceAccessException — 워커가 재시도 대상으로 처리한다.
     * confirm/release는 Image Server에서 DB 갱신과(release는) 최대 20건의 R2 삭제뿐이라 5초면 충분하다.
     */
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    @Autowired
    public ImageServerClient(
            @Value("${image-server.url}") final String imageServerUrl,
            @Value("${image-server.api-key}") final String apiKey) {
        this(imageServerUrl, apiKey, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /** 테스트에서 짧은 타임아웃을 주입하기 위한 생성자 */
    ImageServerClient(final String imageServerUrl, final String apiKey,
            final Duration connectTimeout, final Duration readTimeout) {
        // 타임아웃 지정 전에도 RestClient 기본값이 JDK HttpClient(JdkClientHttpRequestFactory)였다 —
        // 같은 구현을 명시하고 타임아웃만 얹어 전송 방식은 바꾸지 않는다(기본값은 타임아웃이 없음)
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(imageServerUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 이미지를 PENDING -> COMMITTED로 확정하고, 확정되지 않은 id 목록을 반환한다(전부 확정되면 빈 목록).
     * Image Server는 이미 지워졌거나(24시간 만료 GC 등) 없는 id를 에러 없이 빼고 200을 주므로, 응답의
     * confirmed와 요청을 비교해야 누락을 알 수 있다 — 누락분은 다시 해도 돌아오지 않는 영구 실패다.
     * @throws RestClientException HTTP 오류(연결 실패·타임아웃·4xx·5xx), 또는 200인데 본문을 읽지 못함 —
     *         본문을 못 읽은 경우를 "전부 누락"으로 돌려주면 정상 이미지까지 영구 실패로 오판하므로 예외로 알린다
     */
    public List<String> confirm(final List<String> ids) {
        if (ids.isEmpty())
            return List.of();
        final String body = restClient.post()
                .uri("/images/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IdsRequest(ids))
                .retrieve()
                .body(String.class);

        final Set<String> confirmed = parseConfirmed(body);
        return ids.stream().filter(id -> !confirmed.contains(id)).toList();
    }

    /**
     * R2 + Image Server DB에서 즉시 제거. 호출 측(워커)은 post_images로 소유가 확인된 id만 넘긴다.
     * Image Server는 이미 없는 id를 에러 없이 건너뛰므로, 예외 없이 끝나면 요청한 id는 전부 정리된 것으로 본다.
     */
    public void release(final List<String> ids) {
        if (ids.isEmpty())
            return;
        restClient.post()
                .uri("/images/release")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IdsRequest(ids))
                .retrieve()
                .toBodilessEntity();
    }

    private static Set<String> parseConfirmed(final String body) {
        final JsonNode confirmed;
        try {
            confirmed = body == null ? null : MAPPER.readTree(body).get("confirmed");
        } catch (Exception e) {
            throw new RestClientException("Image Server confirm 응답을 읽지 못함", e);
        }
        if (confirmed == null || !confirmed.isArray()) {
            throw new RestClientException("Image Server confirm 응답에 confirmed 목록이 없음");
        }
        final Set<String> ids = new HashSet<>();
        confirmed.forEach(node -> ids.add(node.asText()));
        return ids;
    }

    private record IdsRequest(List<String> ids) {
    }
}
