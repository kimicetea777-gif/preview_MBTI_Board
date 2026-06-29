package com.board.kimicetea.global.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tiptap JSONB content에서 텍스트·이미지 키를 추출하는 유틸리티
 * 주의: content JSON이 null·blank이면 빈 값/null 반환 (예외 없음)
 */
public final class TiptapContentParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_DEPTH = 20;
    public static final String PLACEHOLDER_PREFIX = "{public-url}/";
    // 업로드 엔드포인트가 생성하는 key 패턴만 허용 — 위조된 임의 경로 차단
    private static final Pattern VALID_IMAGE_KEY = Pattern
            .compile("^images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp$");

    private TiptapContentParser() {
    }

    /** Tiptap JSON에서 text 노드만 수집해 maxLength 자로 잘라 반환 */
    public static String extractTextPreview(final String jsonContent, final int maxLength) {
        if (isBlankContent(jsonContent))
            return "";

        return parseJson(jsonContent)
                .map(root -> {
                    final StringBuilder sb = new StringBuilder();
                    collectText(root, sb, 0, maxLength);
                    return sb.toString().trim();
                })
                .map(text -> text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text)
                .orElse("");
    }

    /** Tiptap JSON에서 이미지 key를 문서 순서대로 최대 maxCount개 추출. 이미지가 없으면 null 반환 */
    public static String[] extractImageKeys(final String jsonContent, final int maxCount) {
        if (isBlankContent(jsonContent))
            return null;

        return parseJson(jsonContent)
                .map(root -> {
                    final List<String> keys = new ArrayList<>();
                    collectImageKeys(root, keys, 0, maxCount);
                    return keys;
                })
                .filter(keys -> !keys.isEmpty())
                .map(keys -> keys.toArray(new String[0]))
                .orElse(null);
    }

    /** Tiptap JSON에 포함된 이미지 노드 수를 반환 — 개수 초과 여부 검증에 사용 */
    public static int countImages(final String jsonContent) {
        if (isBlankContent(jsonContent))
            return 0;
        // 파싱 실패 = 유효하지 않은 content → 항상 한도 초과로 처리해 저장 차단
        return parseJson(jsonContent)
                .map(root -> {
                    final int[] count = { 0 };
                    countImageNodes(root, count, 0);
                    return count[0];
                })
                .orElse(Integer.MAX_VALUE);
    }

    // -----------------------------------------------------------------------------------------
    /** DB 저장 전 — 실제 R2 URL을 {public-url}/ 플레이스홀더로 치환 */
    public static String normalizeForStorage(final String content, final String r2PublicUrl) {
        if (content == null || r2PublicUrl == null || r2PublicUrl.isBlank())
            return content;
        return content.replace(r2PublicUrl + "/", PLACEHOLDER_PREFIX);
    }

    /** API 응답 전 — {public-url}/ 플레이스홀더를 실제 R2 URL로 복원 */
    public static String resolveForResponse(final String content, final String r2PublicUrl) {
        if (content == null || r2PublicUrl == null || r2PublicUrl.isBlank())
            return content;
        return content.replace(PLACEHOLDER_PREFIX, r2PublicUrl + "/");
    }

    /** Tiptap JSON에서 자체 R2 이미지 URL만 추출 — content diff 기반 삭제 대상 파악에 사용 */
    public static Set<String> extractImageUrls(final String jsonContent, final String r2PublicUrl) {
        if (isBlankContent(jsonContent)) return Set.of();
        return parseJson(jsonContent)
                .map(root -> {
                    final Set<String> urls = new HashSet<>();
                    collectImageUrls(root, urls, 0, r2PublicUrl);
                    return urls;
                })
                .orElse(Set.of());
    }

    private static void collectImageUrls(final JsonNode node, final Set<String> urls,
            final int depth, final String r2PublicUrl) {
        if (depth > MAX_DEPTH) return;
        if (node.isObject()) {
            if (isNodeType(node, "image")) {
                final String src = node.path("attrs").path("src").asText(null);
                if (src != null && (src.startsWith(r2PublicUrl) || src.startsWith(PLACEHOLDER_PREFIX))) {
                    urls.add(src);
                }
            }
            if (node.has("content")) {
                collectImageUrls(node.path("content"), urls, depth + 1, r2PublicUrl);
            }
        } else if (node.isArray()) {
            for (final JsonNode child : node) {
                collectImageUrls(child, urls, depth + 1, r2PublicUrl);
            }
        }
    }

    // ---------------------------------------------(private)_재귀_순회--------------------------------------------
    private static void countImageNodes(final JsonNode node, final int[] count, final int depth) {
        if (depth > MAX_DEPTH)
            return;
        if (node.isObject()) {
            if (isNodeType(node, "image")) {
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
        if (depth > MAX_DEPTH || sb.length() > maxLength)
            return;
        if (node.isObject()) {
            if (isNodeType(node, "text") && node.has("text")) {
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
            final int depth, final int maxCount) {
        if (depth > MAX_DEPTH || keys.size() >= maxCount)
            return;

        if (node.isArray()) {
            for (final JsonNode child : node) {
                if (keys.size() >= maxCount)
                    break;
                collectImageKeys(child, keys, depth + 1, maxCount);
            }
            return;
        }

        if (!node.isObject())
            return;

        if (isNodeType(node, "image")) {
            extractValidKey(node).ifPresent(keys::add);
            return;
        }

        if (node.has("content")) {
            collectImageKeys(node.path("content"), keys, depth + 1, maxCount);
        }
    }

    // ------------------------------------(private)_Helper-----------------------------------------
    private static Optional<JsonNode> parseJson(final String jsonContent) {
        try {
            return Optional.of(MAPPER.readTree(jsonContent));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> extractValidKey(final JsonNode imageNode) {
        final String src = imageNode.path("attrs").path("src").asText(null);
        if (src == null || !src.startsWith(PLACEHOLDER_PREFIX))
            return Optional.empty();
        final String key = src.substring(PLACEHOLDER_PREFIX.length());
        return VALID_IMAGE_KEY.matcher(key).matches() ? Optional.of(key) : Optional.empty();
    }

    private static boolean isNodeType(final JsonNode node, final String type) {
        return type.equals(node.path("type").asText());
    }

    private static boolean isBlankContent(final String jsonContent) {
        return jsonContent == null || jsonContent.isBlank();
    }
}
