package com.board.kimicetea.global.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tiptap JSONB content에서 텍스트·이미지 키를 추출하는 유틸리티
 * 주의: content JSON이 null·blank이면 빈 값/null 반환 (예외 없음)
 */
public final class TiptapContentParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_DEPTH = 20;
    private static final String PLACEHOLDER_PREFIX = "{public-url}/";
    // 업로드 엔드포인트가 생성하는 key 패턴만 허용 — 위조된 임의 경로 차단
    private static final Pattern VALID_IMAGE_KEY =
            Pattern.compile("^images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp$");

    private TiptapContentParser() {}

    /** Tiptap JSON에서 text 노드만 수집해 maxLength 자로 잘라 반환 */
    public static String extractTextPreview(final String jsonContent, final int maxLength) {
        if (jsonContent == null || jsonContent.isBlank()) return "";
        try {
            final StringBuilder sb = new StringBuilder();
            collectText(MAPPER.readTree(jsonContent), sb, 0, maxLength);
            final String text = sb.toString().trim();
            return text.length() > maxLength ? text.substring(0, maxLength) + "…" : text;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Tiptap JSON에서 이미지 key를 문서 순서대로 최대 maxCount개 추출
     * {public-url}/ prefix를 제거한 key만 저장 (예: images/uuid.webp)
     * 이미지가 없으면 null 반환
     */
    public static String[] extractImageKeys(final String jsonContent, final int maxCount) {
        if (jsonContent == null || jsonContent.isBlank()) return null;
        try {
            final List<String> keys = new ArrayList<>();
            collectImageKeys(MAPPER.readTree(jsonContent), keys, 0, maxCount);
            return keys.isEmpty() ? null : keys.toArray(new String[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private static void collectText(final JsonNode node, final StringBuilder sb, final int depth, final int maxLength) {
        if (depth > MAX_DEPTH || sb.length() > maxLength) return;
        if (node.isObject()) {
            if ("text".equals(node.path("type").asText()) && node.has("text")) {
                sb.append(node.path("text").asText());
            }
            if (node.has("content")) {
                collectText(node.path("content"), sb, depth + 1, maxLength);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                collectText(child, sb, depth + 1, maxLength);
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            }
        }
    }

    private static void collectImageKeys(final JsonNode node, final List<String> keys, final int depth, final int maxCount) {
        if (depth > MAX_DEPTH || keys.size() >= maxCount) return;
        if (node.isObject()) {
            if ("image".equals(node.path("type").asText())) {
                final String src = node.path("attrs").path("src").asText(null);
                if (src != null && src.startsWith(PLACEHOLDER_PREFIX)) {
                    final String key = src.substring(PLACEHOLDER_PREFIX.length());
                    if (VALID_IMAGE_KEY.matcher(key).matches()) {
                        keys.add(key);
                    }
                }
                return;
            }
            if (node.has("content")) {
                collectImageKeys(node.path("content"), keys, depth + 1, maxCount);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                if (keys.size() >= maxCount) break;
                collectImageKeys(child, keys, depth + 1, maxCount);
            }
        }
    }
}
