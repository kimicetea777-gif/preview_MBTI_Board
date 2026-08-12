package com.board.kimicetea.global.image.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Image Server(공용 이미지 업로드 서버)의 confirm/release만 호출하는 얇은 클라이언트.
 * 업로드/취소는 Next.js가 Image Server를 직접 호출하므로 Spring은 이 두 엔드포인트만 필요.
 */
@Component
public class ImageServerClient {

    private final RestClient restClient;

    public ImageServerClient(
            @Value("${image-server.url}") final String imageServerUrl,
            @Value("${image-server.api-key}") final String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(imageServerUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .build();
    }

    /** 게시글 저장 성공 시 호출 — 본문에 포함된 이미지를 PENDING -> COMMITTED로 확정 */
    public void confirm(final List<String> ids) {
        if (ids.isEmpty())
            return;
        restClient.post()
                .uri("/images/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IdsRequest(ids))
                .retrieve()
                .toBodilessEntity();
    }

    /** 게시글 작성 취소·수정으로 빠진 이미지·삭제 시 호출 — R2 + DB에서 즉시 제거 */
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

    private record IdsRequest(List<String> ids) {
    }
}
