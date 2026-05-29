# Phase 9 — Posts 미리보기 & 이미지 개선

> `content_preview`와 `image_urls`를 posts 테이블에 추가한다.
> 목록 조회 시 JSONB 파싱 없이 미리보기 텍스트와 썸네일을 바로 반환할 수 있게 한다.

---

## 설계 결정

### content_preview — 서버 레벨 처리, 300자

`PostSummaryResponse`가 현재 쿼리 시점마다 `extractPreview(post.getContent())`로 JSONB를 파싱 중 (100자 제한).
DB에 `content_preview`를 저장해두면 목록 조회 시 파싱이 없어진다.
`TiptapContentParser.extractTextPreview()`는 기존 `PostSummaryResponse.collectText()` 로직을 그대로 이전한다 — `type === "text"` 조건을 함께 체크해야 정확하므로 `findValuesAsText("text")` 방식은 사용하지 않는다.

### image_urls + content JSONB — `{public-url}` 플레이스홀더 방식

`content` JSONB 안의 `image.attrs.src`와 `image_urls` 컬럼 모두 도메인 변경에 안전하게 만든다.

**저장 시 (프론트 → 서버):**
`submit` 직전 content JSON string에서 `NEXT_PUBLIC_R2_PUBLIC_URL` 값을 `{public-url}` 리터럴로 치환 후 전송.
DB에는 `{public-url}/images/uuid.webp` 형태로 저장된다.

**렌더링/수정 로드 시 (서버 → 프론트):**
TipTap에 content를 넘기기 전 `{public-url}`을 `NEXT_PUBLIC_R2_PUBLIC_URL` 값으로 역치환.
에디터가 실제 URL로 이미지를 표시한다.

**에디터 편집 중:**
이미지 업로드 직후 `setImage({ src: fullUrl })`로 full URL 유지 — 에디터 내부 미리보기에 필요.
치환은 submit 시점에만 수행하므로 편집 경험에 영향 없다.

**image_urls 컬럼:**
백엔드 `TiptapContentParser`가 content에서 image src를 추출할 때 `{public-url}/` prefix를 제거하고 key만 저장.
API 응답 시 `r2PublicUrl + "/" + key`로 조합해 반환. 조합은 `PostSummaryResponse.from(post, r2PublicUrl)` 시그니처로 처리.
`.env`의 `R2_PUBLIC_URL`만 바꾸면 content · image_urls 모두 자동 갱신된다.

---

## [Backend]

### 9-1. V1 스키마 수정 (Flyway 초기화 기준)

Flyway를 초기화하면 V2~V6가 모두 사라지므로 **V1에 전부 통합**한다.

| 기존 파일 | 처리 |
|-----------|------|
| V1 `init_schema.sql` | 수정 — `posts`에 두 컬럼 추가, `deleted_images` 테이블 추가 |
| V2 mbti 시드 | V1에 통합 |
| V3 post_types 시드 | V1에 통합 |
| V4 mbti_questions 시드 | V1에 통합 |
| V5 더미 데이터 | 개발용 — V1에 통합하되 별도 파일 유지 여부는 선택 |
| V6 `deleted_images` | V1에 통합 |

`posts` 테이블 생성문에 추가할 컬럼:

```sql
content_preview  VARCHAR(300),
image_urls       TEXT[]
    CHECK (image_urls IS NULL OR array_length(image_urls, 1) <= 3),
```

`deleted_images` 테이블 (기존 V6 내용):

```sql
CREATE TABLE deleted_images (
    id        BIGSERIAL PRIMARY KEY,
    url       TEXT        NOT NULL,
    marked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_deleted_images_marked_at ON deleted_images (marked_at);
```

### 9-2. Post 엔티티 수정

- `contentPreview` — `@Column(name = "content_preview", length = 300)`
- `imageUrls` — `@Column(name = "image_urls", columnDefinition = "TEXT[]")` + `@Type(StringArrayType.class)`
- `build.gradle`에 `io.hypersistence:hypersistence-utils-hibernate-63` 의존성 추가

> `r2.public-url`은 `application.yaml`에 이미 설정되어 있으므로 추가 작업 불필요.

### 9-3. TiptapContentParser 유틸리티 신규 추가

- 신규 클래스: `TiptapContentParser.java` (패키지: `global/util`)
- `extractTextPreview(JsonNode content, int maxLength)` — 기존 `PostSummaryResponse.collectText()` 로직을 이전. `type === "text"` 조건 + `text` 필드 존재 여부 함께 체크
- `extractImageKeys(JsonNode content, int maxCount)` — `content` 배열을 문서 순서대로 재귀 순회, `type=image` 노드의 `attrs.src`에서 `{public-url}/` prefix를 제거한 key를 최대 maxCount개 추출 (`imageUrls[0]`이 문서 최상단 이미지가 됨)

### 9-4. PostService 수정

**저장/수정 시** — content 파싱 후 preview·imageKeys 세팅:

```java
String preview = TiptapContentParser.extractTextPreview(contentJson, 300);
String[] imageKeys = TiptapContentParser.extractImageKeys(contentJson, 3);
post.setContentPreview(preview);
post.setImageUrls(imageKeys);
```

**삭제 시** — 삭제된 게시글은 복구 없음 정책. `post.hide()` 전에 모든 이미지 삭제 예약:

```java
imageCleanupService.markRemovedImages(post.getContent(), "");
post.hide();
```

### 9-5. ImageCleanupService 수정

`{public-url}` 플레이스홀더 방식으로 content의 image src 형태가 변경되므로 두 곳 수정:

- `collectImageUrls()` — `src.startsWith(r2PublicUrl)` 조건에 `src.startsWith("{public-url}")` 조건 추가
- `keyFromUrl()` — `{public-url}/` prefix 제거 처리 추가

### 9-6. PostSummaryResponse 수정

- `imageUrls: List<String>` 필드 추가
- `from(Post post)` → `from(Post post, String r2PublicUrl)` 시그니처 변경
  - `extractPreview(post.getContent())` → `post.getContentPreview()` 로 교체 (DB 저장값 사용)
  - `imageUrls`: `post.getImageUrls()`의 각 key에 `r2PublicUrl + "/" + key`로 조합
  - 기존 `extractPreview()` · `collectText()` 스태틱 메서드 제거
- `PostService`에서 `PostSummaryResponse.from(post, r2PublicUrl)` 호출로 변경

> `PostRepositoryImpl`은 `Post` 엔티티 전체를 fetchJoin으로 가져오므로 **수정 불필요**.

---

## [Frontend]

### 9-7. `.env.local` 및 `.env.example` 수정

- `NEXT_PUBLIC_R2_PUBLIC_URL` 항목 추가 (`R2_PUBLIC_URL`과 동일한 값)
- `NEXT_PUBLIC_` prefix가 있어야 브라우저에서 접근 가능

```
NEXT_PUBLIC_R2_PUBLIC_URL=https://pub-xxx.r2.dev
```

### 9-8. `lib/r2Utils.ts` 신규 추가

```ts
const R2_URL = process.env.NEXT_PUBLIC_R2_PUBLIC_URL!;
const PLACEHOLDER = "{public-url}";

export function encodeR2Urls(content: string): string {
  return content.replaceAll(R2_URL, PLACEHOLDER);
}

export function decodeR2Urls(content: string): string {
  return content.replaceAll(PLACEHOLDER, R2_URL);
}
```

### 9-9. 게시글 저장/수정 API 호출 시 encode 적용

- `lib/api/post.ts` — `createPost`, `updatePost` 호출 전 content에 `encodeR2Urls` 적용

### 9-10. 게시글 렌더링/수정 로드 시 decode 적용

- 게시글 상세 페이지 — content를 TipTap 렌더러에 넘기기 전 `decodeR2Urls` 적용
- 수정 페이지 — `RichContentEditor`의 `initialContent` prop에 넘기기 전 `decodeR2Urls` 적용

### 9-11. `next.config.ts` 확인

- `images.remotePatterns`에 `NEXT_PUBLIC_R2_PUBLIC_URL` 도메인이 허용되어 있는지 확인

### 9-12. 타입 수정

- `types/post.ts` — `PostSummaryResponse`에 `imageUrls: string[] | null` 추가
  (`contentPreview`는 이미 존재하므로 확인 후 없으면 추가)

### 9-13. 게시글 카드 UI 개선

- `PostCard` 컴포넌트
  - `contentPreview` 2줄 말줄임 표시
  - `imageUrls[0]` 존재 시 썸네일 이미지 표시

---

## 개발 순서

```
9-1 V1 스키마 수정 (posts 컬럼 추가 + deleted_images + 시드 통합)
 → 9-2 엔티티
 → 9-3 TiptapContentParser 유틸
 → 9-4 PostService
 → 9-5 ImageCleanupService
 → 9-6 PostSummaryResponse
 → 통합 테스트 (작성/수정/삭제 시 preview·imageKeys·이미지 예약 검증)
 → 9-7 .env.local · .env.example
 → 9-8 r2Utils.ts
 → 9-9 encode 적용 (저장)
 → 9-10 decode 적용 (렌더링/수정 로드)
 → 9-11 next.config.ts 확인
 → 9-12 타입
 → 9-13 카드 UI
```
