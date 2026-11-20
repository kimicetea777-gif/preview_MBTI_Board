package com.board.kimicetea.global.membershipsync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [역할] AS 응답 JSON이 {@link MembershipEventsPollResponse}로 어떻게 읽히는지 고정한다.
 * [주의사항] {@link MembershipEventPollClient}도 {@code RestClient.create(url)}의 기본 변환기로 읽으므로
 * 같은 {@code Jackson2ObjectMapperBuilder.json().build()}로 검증한다(DeletedUsersPollResponseJsonTest와 같은 이유).
 */
class MembershipEventsPollResponseJsonTest {

    private static final String ITEM =
            "{\"type\":\"CLIENT_WITHDRAWN\",\"id\":42,\"userId\":\"9c1b6f3e-0d2a-4b8e-9f1a-2c3d4e5f6a7b\","
                    + "\"clientId\":\"TypeButter\",\"changedAt\":\"2026-08-07T04:00:03.120Z\"}";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("hasNext가 없는 옛 AS 응답이면 hasNext는 null")
    void hasNextIsNullForResponsesFromAnAsWithoutTheField() throws Exception {
        final MembershipEventsPollResponse response =
                objectMapper.readValue("{\"items\":[" + ITEM + "]}", MembershipEventsPollResponse.class);

        assertThat(response.items()).hasSize(1);
        assertThat(response.hasNext()).isNull();
    }

    @Test
    @DisplayName("hasNext가 있으면 그 값을 그대로 읽는다")
    void readsHasNextWhenPresent() throws Exception {
        final MembershipEventsPollResponse response =
                objectMapper.readValue("{\"items\":[" + ITEM + "],\"hasNext\":true}", MembershipEventsPollResponse.class);

        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("AS가 응답이나 항목에 모르는 필드를 더 보내도 파싱이 깨지지 않는다")
    void ignoresUnknownFieldsFromAs() throws Exception {
        final String itemWithExtra = ITEM.replace("}", ",\"newItemField\":\"x\"}");
        final MembershipEventsPollResponse response = objectMapper.readValue(
                "{\"items\":[" + itemWithExtra + "],\"hasNext\":false,\"newTopLevelField\":1}",
                MembershipEventsPollResponse.class);

        assertThat(response.items()).extracting(MembershipEventResponse::id).containsExactly(42L);
        assertThat(response.hasNext()).isFalse();
    }
}
