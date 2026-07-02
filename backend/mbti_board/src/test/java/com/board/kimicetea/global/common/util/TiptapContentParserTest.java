package com.board.kimicetea.global.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TiptapContentParserTest {

    private static final String R2_URL = "https://r2.example.com";
    private static final String PLACEHOLDER = TiptapContentParser.PLACEHOLDER_PREFIX;

    private static final String VALID_IMAGE_KEY = "images/550e8400-e29b-41d4-a716-446655440000.webp";
    private static final String VALID_IMAGE_KEY_2 = "images/6ba7b810-9dad-11d1-80b4-00c04fd430c8.webp";

    // ── 공통 JSON 픽스처 ──────────────────────────────────────────────

    private static String textDoc(final String text) {
        return """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted(text).strip();
    }

    private static String imageDoc(final String... srcs) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"doc\",\"content\":[");
        for (int i = 0; i < srcs.length; i++) {
            sb.append("{\"type\":\"image\",\"attrs\":{\"src\":\"%s\"}}".formatted(srcs[i]));
            if (i < srcs.length - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ── extractTextPreview ────────────────────────────────────────────

    @Nested
    @DisplayName("extractTextPreview — 텍스트 미리보기 추출")
    class ExtractTextPreview {

        @Test
        @DisplayName("null 입력이면 빈 문자열을 반환한다")
        void null_returnsEmpty() {
            assertThat(TiptapContentParser.extractTextPreview(null, 100)).isEmpty();
        }

        @Test
        @DisplayName("공백 입력이면 빈 문자열을 반환한다")
        void blank_returnsEmpty() {
            assertThat(TiptapContentParser.extractTextPreview("  ", 100)).isEmpty();
        }

        @Test
        @DisplayName("유효하지 않은 JSON이면 빈 문자열을 반환한다")
        void invalidJson_returnsEmpty() {
            assertThat(TiptapContentParser.extractTextPreview("not-json", 100)).isEmpty();
        }

        @Test
        @DisplayName("텍스트가 maxLength 이내이면 그대로 반환한다")
        void shortText_returnsFull() {
            final String content = textDoc("안녕하세요");
            assertThat(TiptapContentParser.extractTextPreview(content, 100)).isEqualTo("안녕하세요");
        }

        @Test
        @DisplayName("텍스트가 maxLength 초과이면 말줄임표로 잘라 반환한다")
        void longText_truncatedWithEllipsis() {
            final String content = textDoc("가".repeat(200));
            final String result = TiptapContentParser.extractTextPreview(content, 10);

            assertThat(result).hasSizeLessThanOrEqualTo(10).endsWith("…");
        }
    }

    // ── extractImageKeys ──────────────────────────────────────────────

    @Nested
    @DisplayName("extractImageKeys — 이미지 key 추출")
    class ExtractImageKeys {

        @Test
        @DisplayName("null 입력이면 null을 반환한다")
        void null_returnsNull() {
            assertThat(TiptapContentParser.extractImageKeys(null, 5)).isNull();
        }

        @Test
        @DisplayName("이미지가 없으면 null을 반환한다")
        void noImages_returnsNull() {
            assertThat(TiptapContentParser.extractImageKeys(textDoc("텍스트만"), 5)).isNull();
        }

        @Test
        @DisplayName("유효한 이미지 key를 반환한다")
        void validImageKey_returned() {
            final String src = PLACEHOLDER + VALID_IMAGE_KEY;
            final String content = imageDoc(src);

            final String[] keys = TiptapContentParser.extractImageKeys(content, 5);

            assertThat(keys).containsExactly(VALID_IMAGE_KEY);
        }

        @Test
        @DisplayName("maxCount를 초과하는 이미지는 잘라낸다")
        void exceedsMaxCount_truncated() {
            final String src1 = PLACEHOLDER + VALID_IMAGE_KEY;
            final String src2 = PLACEHOLDER + VALID_IMAGE_KEY_2;
            final String content = imageDoc(src1, src2);

            final String[] keys = TiptapContentParser.extractImageKeys(content, 1);

            assertThat(keys).hasSize(1).containsExactly(VALID_IMAGE_KEY);
        }

        @Test
        @DisplayName("플레이스홀더 없는 외부 이미지 URL은 포함하지 않는다")
        void externalUrl_excluded() {
            final String content = imageDoc("https://external.com/image.png");

            assertThat(TiptapContentParser.extractImageKeys(content, 5)).isNull();
        }

        @Test
        @DisplayName("위조된 경로(패턴 불일치)는 포함하지 않는다")
        void invalidKeyPattern_excluded() {
            final String content = imageDoc(PLACEHOLDER + "images/../../etc/passwd");

            assertThat(TiptapContentParser.extractImageKeys(content, 5)).isNull();
        }
    }

    // ── countImages ───────────────────────────────────────────────────

    @Nested
    @DisplayName("countImages — 이미지 노드 수 카운트")
    class CountImages {

        @Test
        @DisplayName("null 입력이면 0을 반환한다")
        void null_returnsZero() {
            assertThat(TiptapContentParser.countImages(null)).isZero();
        }

        @Test
        @DisplayName("이미지 없으면 0을 반환한다")
        void noImages_returnsZero() {
            assertThat(TiptapContentParser.countImages(textDoc("텍스트"))).isZero();
        }

        @Test
        @DisplayName("이미지 2개면 2를 반환한다")
        void twoImages_returnsTwo() {
            final String content = imageDoc(
                    PLACEHOLDER + VALID_IMAGE_KEY,
                    PLACEHOLDER + VALID_IMAGE_KEY_2);

            assertThat(TiptapContentParser.countImages(content)).isEqualTo(2);
        }

        @Test
        @DisplayName("유효하지 않은 JSON이면 Integer.MAX_VALUE를 반환한다")
        void invalidJson_returnsMaxValue() {
            assertThat(TiptapContentParser.countImages("not-json")).isEqualTo(Integer.MAX_VALUE);
        }
    }

    // ── normalizeForStorage / resolveForResponse ──────────────────────

    @Nested
    @DisplayName("normalizeForStorage — R2 URL을 플레이스홀더로 치환")
    class NormalizeForStorage {

        @Test
        @DisplayName("content에 포함된 R2 URL을 플레이스홀더로 치환한다")
        void replacesR2UrlWithPlaceholder() {
            final String content = "이미지: " + R2_URL + "/" + VALID_IMAGE_KEY;

            final String result = TiptapContentParser.normalizeForStorage(content, R2_URL);

            assertThat(result).contains(PLACEHOLDER + VALID_IMAGE_KEY).doesNotContain(R2_URL);
        }

        @Test
        @DisplayName("content가 null이면 null을 반환한다")
        void nullContent_returnsNull() {
            assertThat(TiptapContentParser.normalizeForStorage(null, R2_URL)).isNull();
        }
    }

    @Nested
    @DisplayName("resolveForResponse — 플레이스홀더를 R2 URL로 복원")
    class ResolveForResponse {

        @Test
        @DisplayName("플레이스홀더를 실제 R2 URL로 복원한다")
        void replacesPlaceholderWithR2Url() {
            final String content = "이미지: " + PLACEHOLDER + VALID_IMAGE_KEY;

            final String result = TiptapContentParser.resolveForResponse(content, R2_URL);

            assertThat(result).contains(R2_URL + "/" + VALID_IMAGE_KEY).doesNotContain(PLACEHOLDER);
        }

        @Test
        @DisplayName("normalizeForStorage → resolveForResponse 왕복 후 원본과 동일하다")
        void roundTrip_restoresOriginal() {
            final String original = "이미지: " + R2_URL + "/" + VALID_IMAGE_KEY;

            final String normalized = TiptapContentParser.normalizeForStorage(original, R2_URL);
            final String restored = TiptapContentParser.resolveForResponse(normalized, R2_URL);

            assertThat(restored).isEqualTo(original);
        }
    }
}
