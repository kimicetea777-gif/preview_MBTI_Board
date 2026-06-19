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
            return text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Tiptap JSON에서 이미지 key를 문서 순서대로 최대 maxCount개 추출
     * {public-url}/ 또는 실제 r2PublicUrl/ prefix 모두 처리 (저장 방식 호환)
     * 이미지가 없으면 null 반환
     */
    public static String[] extractImageKeys(final String jsonContent, final int maxCount, final String r2PublicUrl) {
        if (jsonContent == null || jsonContent.isBlank()) return null;
        try {
            final List<String> keys = new ArrayList<>();
            collectImageKeys(MAPPER.readTree(jsonContent), keys, 0, maxCount, r2PublicUrl);
            return keys.isEmpty() ? null : keys.toArray(new String[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /** r2PublicUrl 없이 호출하는 기존 경로 — {public-url}/ prefix만 처리 */
    public static String[] extractImageKeys(final String jsonContent, final int maxCount) {
        return extractImageKeys(jsonContent, maxCount, null);
    }

    /** Tiptap JSON에 포함된 이미지 노드 수를 반환 — 개수 초과 여부 검증에 사용 */
    public static int countImages(final String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) return 0;
        try {
            final int[] count = {0};
            countImageNodes(MAPPER.readTree(jsonContent), count, 0);
            return count[0];
        } catch (Exception e) {
            // 파싱 실패 = 유효하지 않은 content → 항상 한도 초과로 처리해 저장 차단
            return Integer.MAX_VALUE;
        }
    }

    private static void countImageNodes(final JsonNode node, final int[] count, final int depth) {
        if (depth > MAX_DEPTH) return;
        if (node.isObject()) {
            if ("image".equals(node.path("type").asText())) {
                count[0]++;
                return;
            }
            if (node.has("content")) {
                countImageNodes(node.path("content"), count, depth + 1);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                countImageNodes(child, count, depth + 1);
            }
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

    private static void collectImageKeys(final JsonNode node, final List<String> keys,
            final int depth, final int maxCount, final String r2PublicUrl) {
        if (depth > MAX_DEPTH || keys.size() >= maxCount) return;
        if (node.isObject()) {
            if ("image".equals(node.path("type").asText())) {
                final String src = node.path("attrs").path("src").asText(null);
                if (src != null) {
                    String key = null;
                    if (src.startsWith(PLACEHOLDER_PREFIX)) {
                        key = src.substring(PLACEHOLDER_PREFIX.length());
                    } else if (r2PublicUrl != null && src.startsWith(r2PublicUrl + "/")) {
                        key = src.substring(r2PublicUrl.length() + 1);
                    }
                    if (key != null && VALID_IMAGE_KEY.matcher(key).matches()) {
                        keys.add(key);
                    }
                }
                return;
            }
            if (node.has("content")) {
                collectImageKeys(node.path("content"), keys, depth + 1, maxCount, r2PublicUrl);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                if (keys.size() >= maxCount) break;
                collectImageKeys(child, keys, depth + 1, maxCount, r2PublicUrl);
            }
        }
    }
}
