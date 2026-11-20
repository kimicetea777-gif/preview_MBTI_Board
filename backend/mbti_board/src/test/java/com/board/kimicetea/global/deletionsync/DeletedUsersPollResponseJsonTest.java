package com.board.kimicetea.global.deletionsync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [역할] AS 응답 JSON이 {@link DeletedUsersPollResponse}로 어떻게 읽히는지 고정한다.
 * [주의사항] {@link DeletedUserPollClient}는 {@code RestClient.create(url)}을 쓰므로 Boot의 ObjectMapper가 아니라
 * Spring 기본 변환기(MappingJackson2HttpMessageConverter)의 ObjectMapper로 읽는다 — 그 변환기가 만드는 것과 같은
 * {@code Jackson2ObjectMapperBuilder.json().build()}로 검증한다.
 */
class DeletedUsersPollResponseJsonTest {

    private static final String ITEM =
            "{\"type\":\"ACCOUNT_DELETED\",\"id\":101,\"userId\":\"9c1b6f3e-0d2a-4b8e-9f1a-2c3d4e5f6a7b\","
                    + "\"deletedAt\":\"2026-08-01T04:00:03.120Z\"}";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("hasNext가 없는 옛 AS 응답이면 hasNext는 null — 원시 boolean이었다면 false가 돼 첫 페이지에서 멈춘다")
    void hasNextIsNullForResponsesFromAnAsWithoutTheField() throws Exception {
        final DeletedUsersPollResponse response =
                objectMapper.readValue("{\"items\":[" + ITEM + "]}", DeletedUsersPollResponse.class);

        assertThat(response.items()).hasSize(1);
        assertThat(response.hasNext()).isNull();
    }

    @Test
    @DisplayName("hasNext가 있으면 그 값을 그대로 읽는다")
    void readsHasNextWhenPresent() throws Exception {
        final DeletedUsersPollResponse response =
                objectMapper.readValue("{\"items\":[" + ITEM + "],\"hasNext\":true}", DeletedUsersPollResponse.class);

        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("AS가 응답이나 항목에 모르는 필드를 더 보내도 파싱이 깨지지 않는다")
    void ignoresUnknownFieldsFromAs() throws Exception {
        final String itemWithExtra = ITEM.replace("}", ",\"newItemField\":\"x\"}");
        final DeletedUsersPollResponse response = objectMapper.readValue(
                "{\"items\":[" + itemWithExtra + "],\"hasNext\":false,\"newTopLevelField\":1}",
                DeletedUsersPollResponse.class);

        assertThat(response.items()).extracting(DeletedUserEventResponse::id).containsExactly(101L);
        assertThat(response.hasNext()).isFalse();
    }
}
