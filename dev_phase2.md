# Phase 2 — Post 도메인 (FREE 게시판) Backend

> 구현 완료 기준으로 작성. 실제 코드와 일치하도록 유지.

---

## 구현된 파일 목록

```
domain/post/
├── repository/
│   ├── PostRepositoryCustom.java     (QueryDSL 커스텀 인터페이스)
│   ├── PostRepositoryImpl.java       (QueryDSL 구현체)
│   ├── PostRepository.java           (JpaRepository + @Modifying)
│   └── PostTypeRepository.java
├── dto/
│   ├── PostCreateRequest.java
│   ├── PostUpdateRequest.java
│   ├── PostDeleteRequest.java
│   ├── PostSummaryResponse.java      (목록용)
│   └── PostDetailResponse.java       (상세용)
├── service/
│   └── PostService.java
└── controller/
    └── PostController.java

global/
└── config/
    └── JpaConfig.java                (JPAQueryFactory 빈)

db/migration/
├── V1__init_schema.sql               (posts 테이블 포함)
├── V2__seed_mbti_metadata.sql        (NONE MBTI 포함)
└── V3__add_target_mbti_to_posts.sql  (target_mbti 컬럼 추가)
```

---

## API 명세

| Method   | URL               | 인증               | 설명             |
| -------- | ----------------- | ------------------ | ---------------- |
| `GET`    | `/api/posts`      | 불필요             | 게시글 목록 조회 |
| `GET`    | `/api/posts/{id}` | 불필요             | 게시글 단건 조회 |
| `POST`   | `/api/posts`      | 선택 (비회원 가능) | 게시글 작성      |
| `PATCH`  | `/api/posts/{id}` | 선택 (비회원 가능) | 게시글 수정      |
| `DELETE` | `/api/posts/{id}` | 선택 (비회원 가능) | 게시글 삭제      |

### GET /api/posts — 쿼리 파라미터

| 파라미터     | 타입   | 기본값   | 제약                | 설명                            |
| ------------ | ------ | -------- | ------------------- | ------------------------------- |
| `type`       | String | `FREE`   | -                   | 게시판 유형 코드                |
| `sort`       | String | `latest` | -                   | `latest` / `comments` / `views` |
| `authorMbti` | String | (없음)   | -                   | 작성자 MBTI 필터                |
| `targetMbti` | String | `ALL`    | -                   | 타겟 MBTI 필터 (`ALL` = 전체)   |
| `page`       | int    | `0`      | `@Min(0)`           | 페이지 번호                     |
| `size`       | int    | `20`     | `@Min(1) @Max(100)` | 페이지 크기                     |

### DELETE /api/posts/{id} — 비회원 비밀번호 전달 방식

```
X-Guest-Password: abc123   ← 커스텀 헤더로 전달 (body 아님)
```

> `@RequestBody`를 쓰면 Cloudflare Tunnel 등 일부 프록시가 DELETE body를 제거함.
> 헤더 방식으로 안전하게 전달. 회원은 헤더 생략.

---

## DTO 상세

### PostCreateRequest

```java
@NotBlank @Size(max = 150)   String title
@NotBlank @Size(max = 50_000) String content
@NotBlank                    String type          // FREE / VOTE / DEBATE
@Size(min = 4, max = 72)     String guestPassword // 비회원 전용, BCrypt 72바이트 한계
                             String targetMbti    // null/"ALL" = 전체, "INFP" 등 = 특정 타겟
```

### PostUpdateRequest

```java
@NotBlank @Size(max = 150)   String title
@NotBlank @Size(max = 50_000) String content
@Size(min = 4, max = 72)     String guestPassword // 비회원 전용
```

### PostDeleteRequest

```java
String guestPassword  // X-Guest-Password 헤더값을 서비스로 전달하는 내부 래퍼
```

### PostSummaryResponse (목록)

```java
Long id
String title
String authorNickname    // user == null → "(탈퇴한 사용자)"
String authorMbtiId      // MbtiMetadata.id (ex: "INFP")
String authorMbtiColor   // MbtiMetadata.colorCode
String mbtiVerified      // MbtiVerified.name()
String mbtiVerifiedColor // MbtiVerified.colorCode
String targetMbtiId      // null = 전체 대상
int viewCount
int commentCount
Instant createdAt
```

### PostDetailResponse (상세)

```java
// PostSummaryResponse 모든 필드 +
String content       // Tiptap JSON 문자열
boolean isOwner      // 현재 요청자가 작성자인지 (JWT 없으면 false)
boolean isGuest      // 비회원 작성 글 여부 (guestPassword != null)
Instant updatedAt
```

---

## Repository 핵심 쿼리

### PostRepository

```java
// 단건 조회 — isVisible = true 조건 포함, 연관 엔티티 전체 fetchJoin
@Query("SELECT p FROM Post p JOIN FETCH p.postType JOIN FETCH p.authorMbti
        LEFT JOIN FETCH p.user LEFT JOIN FETCH p.targetMbti
        WHERE p.id = :id AND p.isVisible = true")
Optional<Post> findWithDetailsById(Long id);

// viewCount 원자적 증가 — 엔티티 직접 수정 금지
@Modifying
@Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
void increaseViewCount(Long id);
```

### PostRepositoryImpl (QueryDSL)

```java
// targetMbti fetchJoin 포함 — 누락 시 N+1 발생
.join(post.postType).fetchJoin()
.join(post.authorMbti).fetchJoin()
.leftJoin(post.user).fetchJoin()
.leftJoin(post.targetMbti).fetchJoin()

// targetMbti 필터 — "ALL"이면 스킵, 특정 MBTI면 eq 조건 추가
if (!"ALL".equals(targetMbti)) {
    builder.and(post.targetMbti.id.eq(targetMbti));
}
```

---

## 비회원 처리 분기

| 상황                               | 처리                                                                       |
| ---------------------------------- | -------------------------------------------------------------------------- |
| 작성 시 `SecurityContext` 비어있음 | `user = null`, `authorMbti = NONE`, `guestPassword` BCrypt 해싱 저장       |
| 수정 시 비회원                     | `X-Guest-Password` 헤더 → `PostUpdateRequest.guestPassword` 로 BCrypt 검증 |
| 삭제 시 비회원                     | `X-Guest-Password` 헤더 → `PostDeleteRequest.guestPassword` 로 BCrypt 검증 |
| 수정/삭제 시 회원                  | `post.getUser().getId()` vs JWT 유저 UUID 비교, 불일치 시 403              |
| 탈퇴 회원 작성 글                  | `user = null`, `guestPassword = null` → 수정/삭제 불가 (403)               |

---

## target_mbti 설계

- DB: `posts.target_mbti` — nullable FK → `mbti_metadata.id`
- `null` = 전체 대상 (ALL)
- 작성 시 `targetMbti` 파라미터로 지정, 이후 수정 불가 (`Post.update()`에 포함 안 됨)
- 필터링: `GET /api/posts?targetMbti=INFP` → 해당 MBTI 타겟 글만 반환

---

## 주요 주의사항

- `viewCount` — `insertable = false, updatable = false`. `@Modifying` 쿼리로만 증가. 단건 조회 응답에는 증가 전 값이 반영됨 (다음 요청부터 반영)
- `commentCount` — DB 트리거가 관리. JPA 직접 수정 금지
- `content` — `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"`. Tiptap JSON 그대로 저장, 서버 구조 검증 없음
- `authorMbti` — 비회원은 반드시 `NONE` MbtiMetadata FK. V2 마이그레이션에서 시드됨
- `@Validated` on `PostController` — `@Min/@Max` 위반 시 `ConstraintViolationException` → `GlobalExceptionHandler`가 400 반환
- Phase 2는 FREE 게시판만. VOTE/DEBATE 확장은 Phase 4~5

---

## ErrorCode (추가분)

```java
POST_NOT_FOUND(HttpStatus.NOT_FOUND,       "게시글을 찾을 수 없습니다.")
POST_FORBIDDEN(HttpStatus.FORBIDDEN,       "게시글 수정/삭제 권한이 없습니다.")
POST_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST,"존재하지 않는 게시판 유형입니다.")
GUEST_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "비회원은 비밀번호를 입력해야 합니다.")
GUEST_PASSWORD_MISMATCH(HttpStatus.FORBIDDEN,   "비밀번호가 일치하지 않습니다.")
```

---

## 보안 체크리스트

- [x] SQL Injection — QueryDSL 파라미터 바인딩
- [x] XSS — Tiptap JSON 구조상 script 삽입 불가, 렌더링은 프론트 read-only 뷰어
- [x] 소유권 검증 — 회원(UUID 비교) / 비회원(BCrypt) / 탈퇴(403) 모두 처리
- [x] BCrypt 72바이트 한계 — `@Size(max = 72)` 적용
- [x] 대용량 페이로드 — `@Size(max = 50_000)` on content
- [x] 페이징 DOS — `@Max(100)` on size 파라미터
- [x] N+1 — 목록·상세 모두 fetchJoin 완비 (targetMbti 포함)
- [x] ConstraintViolationException — GlobalExceptionHandler 처리
- [x] DELETE body 제거 — X-Guest-Password 헤더로 전환

---

# Phase 2 — Post 도메인 (FREE 게시판) Frontend

> Phase 2 Backend 완료 기준으로 작성. 기존 Phase 0·1 기반(axios, TanStack Query, 공통 컴포넌트) 위에 구현.

---

## 구현할 파일 목록

```
frontend/mbti_board/
├── types/
│   └── post.ts                          (기존 Post·CreatePostRequest 삭제 후 전면 교체)
├── lib/
│   ├── routes.ts                        (freePost 상세 라우트 추가)
│   ├── apiEndpoints.ts                  (posts 섹션 추가)
│   └── api/
│       └── post.ts                      (신규: fetchPosts, fetchPostById, createPost, updatePost, deletePost)
├── hooks/
│   └── usePosts.ts                      (신규: usePostList, usePostDetail)
├── components/
│   └── post/
│       ├── PostCard.tsx                 (신규: 목록 카드)
│       ├── PostListFilter.tsx           (신규: 정렬 탭 + MBTI 필터 드롭다운)
│       ├── RichContentEditor.tsx        (신규: Tiptap 에디터, 작성/수정 전용)
│       ├── RichContentViewer.tsx        (신규: Tiptap read-only 뷰어)
│       └── GuestPasswordModal.tsx       (신규: 비회원 비밀번호 입력 모달)
└── app/(main)/board/free/
    ├── page.tsx                         (신규: 자유게시판 목록)
    ├── write/
    │   └── page.tsx                     (신규: 게시글 작성)
    └── [id]/
        └── page.tsx                     (신규: 게시글 상세 + 수정/삭제)
```

> **CLAUDE.md 규칙**: 위 신규 컴포넌트 5개(`PostCard`, `PostListFilter`, `RichContentEditor`, `RichContentViewer`, `GuestPasswordModal`) 각각 생성 시, `frontend/mbti_board/function_design/componet_descript.md`에 한 줄 요약을 추가해야 한다.

---

## 사전 준비 — Tiptap 설치

```bash
npm install @tiptap/react @tiptap/pm @tiptap/starter-kit @tiptap/extension-placeholder
```

- `@tiptap/starter-kit` — Bold, Italic, Heading, BulletList, Code 등 기본 확장 번들
- `@tiptap/extension-placeholder` — 빈 에디터 플레이스홀더 텍스트

---

## 1. 타입 교체 (`types/post.ts`)

기존 `Post`, `CreatePostRequest`, `VoteOptionRequest`, `VoteOption`, `VoteResult`, `DebateSummary`를 전부 **삭제**하고 아래 타입으로 교체한다.  
(기존 `Post.content: object`는 실제 백엔드 응답과 불일치. `CreatePostRequest`는 명칭·구조 모두 교체.)

```ts
export type PostType = "FREE" | "VOTE" | "DEBATE";

// GET /api/posts 목록 응답
export interface PostSummaryResponse {
  id: number;
  title: string;
  authorNickname: string;          // 탈퇴 시 백엔드가 "(탈퇴한 사용자)"로 치환 — null 불가
  authorMbtiId: string;            // ex: "INFP"
  authorMbtiColor: string;
  mbtiVerified: import("./user").MbtiVerified;
  mbtiVerifiedColor: string;
  targetMbtiId: string | null;     // null = 전체 대상
  viewCount: number;
  commentCount: number;
  createdAt: string;               // ISO 8601
}

// GET /api/posts/{id} 상세 응답
export interface PostDetailResponse extends PostSummaryResponse {
  content: string;                 // Tiptap JSON 직렬화 문자열
  isOwner: boolean;
  isGuest: boolean;
  updatedAt: string;
}

// GET /api/posts 쿼리 파라미터
// sort 값은 백엔드 PostSortType enum과 동일하게 대문자 사용 (LATEST/VIEWS/COMMENTS)
export type PostSortType = "LATEST" | "VIEWS" | "COMMENTS";

export interface PostListParams {
  type: PostType;
  sort?: PostSortType;
  authorMbti?: string;
  targetMbti?: string;             // 생략 또는 "ALL" = 전체
  page?: number;
  size?: number;
}

// POST /api/posts 요청
export interface PostCreateRequest {
  type: PostType;
  title: string;
  content: string;                 // Tiptap JSON 직렬화 문자열
  guestPassword?: string;
  targetMbti?: string;
}

// PATCH /api/posts/{id} 요청
export interface PostUpdateRequest {
  title: string;
  content: string;
  guestPassword?: string;
}
```

> Phase 4~5에서 VOTE/DEBATE 작성 요청에 `voteOptions`, `proLabel` 등 추가 시 별도 인터페이스로 분리.

---

## 2. 라우트 상수 추가 (`lib/routes.ts`)

```ts
board: {
  // 기존 유지
  free: "/board/free",
  vote: "/board/vote",
  debate: "/board/debate",
  freeWrite: "/board/free/write",
  voteWrite: "/board/vote/write",
  debateWrite: "/board/debate/write",
  // 추가
  freePost: (id: number) => `/board/free/${id}`,
},
```

> `PostCard`, 작성 페이지 성공 후 이동, 삭제 후 이동 모두 `ROUTES.board.freePost(id)` 사용.  
> 경로 하드코딩 금지 (CLAUDE.md 규칙).

---

## 3. API 엔드포인트 (`lib/apiEndpoints.ts`)

```ts
posts: {
  list: "/api/posts",
  byId: (id: number) => `/api/posts/${id}`,
},
```

---

## 4. API 함수 (`lib/api/post.ts`)

```ts
// 목록 조회
fetchPosts(params: PostListParams): Promise<PageResponse<PostSummaryResponse>>

// 단건 조회
fetchPostById(id: number): Promise<PostDetailResponse>

// 작성
createPost(data: PostCreateRequest): Promise<PostDetailResponse>

// 수정
updatePost(id: number, data: PostUpdateRequest): Promise<PostDetailResponse>

// 삭제 — 비회원은 guestPassword를 X-Guest-Password 헤더로 전달
deletePost(id: number, guestPassword?: string): Promise<void>
```

### 비회원 DELETE 헤더 처리

```ts
apiClient.delete(API.posts.byId(id), {
  headers: guestPassword ? { "X-Guest-Password": guestPassword } : {},
});
```

---

## 5. 커스텀 훅 (`hooks/usePosts.ts`)

```ts
// 목록 — 무한 스크롤. 필터/정렬 변경 시 자동 리패치
usePostList(params: Omit<PostListParams, "page">)
  → useInfiniteQuery<PageResponse<PostSummaryResponse>>

// 상세
usePostDetail(id: number)
  → useQuery<PostDetailResponse>
```

### usePostList 핵심 옵션

```ts
useInfiniteQuery({
  queryKey: ["posts", params],
  queryFn: ({ pageParam }) => fetchPosts({ ...params, page: pageParam }),
  initialPageParam: 0,
  getNextPageParam: (lastPage) =>
    lastPage.last ? undefined : lastPage.number + 1,
});
```

- `lastPage.last === true` → `undefined` 반환 → `hasNextPage = false` → 스크롤 감지 중단
- 필터/정렬(`params`) 변경 시 쿼리 키가 바뀌어 자동으로 page 0부터 재시작

쿼리 키 설계:

- 목록: `["posts", params]` — `params`에 `page` 미포함 (page는 `pageParam`으로 관리)
- 상세: `["posts", id]`

---

## 6. 컴포넌트 상세

### 6-1. PostCard (`components/post/PostCard.tsx`)

목록에서 게시글 한 건을 표시하는 카드 컴포넌트.

```
props: PostSummaryResponse
```

표시 요소:

- 제목 (클릭 → `ROUTES.board.freePost(post.id)`)
- 작성자 닉네임 + `MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified}` — prop 이름은 `mbti` (컴포넌트 실제 시그니처)
- 타겟 MBTI 배지 (targetMbtiId != null 시)
- 댓글 수 / 조회 수
- 작성일 (상대 시간, ex: "3시간 전")

### 6-2. PostListFilter (`components/post/PostListFilter.tsx`)

정렬 탭 + MBTI 필터 드롭다운.

```
props:
  sort: PostSortType              // "LATEST" | "VIEWS" | "COMMENTS"
  authorMbti: string | undefined
  targetMbti: string | undefined
  onSortChange: (sort: PostSortType) => void
  onAuthorMbtiChange: (mbti: string | undefined) => void
  onTargetMbtiChange: (mbti: string | undefined) => void
```

- 정렬 탭: `최신순(LATEST)` / `댓글많은순(COMMENTS)` / `조회많은순(VIEWS)` — 백엔드 전달값은 대문자 그대로
- 작성자 MBTI 드롭다운: 전체 + 16종 (`fetchMbtiList()` — `lib/api/mbti.ts` 기존 함수 활용, `useQuery` 캐시)
- 타겟 MBTI 드롭다운: 전체(ALL) + 16종

### 6-3. RichContentEditor (`components/post/RichContentEditor.tsx`)

Tiptap 기반 에디터, 작성·수정 페이지 전용.

```
props:
  initialContent?: string   // 수정 시 기존 JSON 문자열
  onChange: (json: string) => void
  placeholder?: string
```

- `useEditor` 훅으로 에디터 인스턴스 생성
- 툴바: Bold / Italic / Heading(H2, H3) / BulletList / Code
- `editor.getJSON()` → `JSON.stringify()` → `onChange` 콜백
- 빈 내용(텍스트 길이 0) 여부를 부모에서 검증

### 6-4. RichContentViewer (`components/post/RichContentViewer.tsx`)

Tiptap read-only 렌더러, 게시글 상세 페이지 전용.

```
props:
  content: string   // JSON 문자열
```

- `useEditor({ editable: false })` + `JSON.parse(content)` → `editor.commands.setContent()`
- EditorContent 컴포넌트로 렌더링
- 에디터 UI(툴바, 커서) 비노출

### 6-5. GuestPasswordModal (`components/post/GuestPasswordModal.tsx`)

비회원 수정·삭제 시 비밀번호 확인 모달.

```
props:
  open: boolean
  action: "update" | "delete"
  onConfirm: (password: string) => void
  onClose: () => void
  isPending: boolean
  error?: string
```

- 비밀번호 입력 `<input type="password">` + 확인 버튼
- `isPending` 시 버튼 비활성화
- `error` 문자열 노출 (GUEST_PASSWORD_MISMATCH 등)

---

## 7. 페이지 상세

### 7-1. 목록 페이지 (`app/(main)/board/free/page.tsx`)

**클라이언트 컴포넌트** (`"use client"`)

```
상태:
  sort: PostSortType  (default: "LATEST")
  authorMbti: string | undefined
  targetMbti: string | undefined
```

렌더링 구조:

```
<section>
  <PostListFilter ... />
  <article>
    {pages.flatMap(page => page.content).map(post =>
      <PostCard key={post.id} {...post} />
    )}
  </article>

  {/* Intersection Observer 감지용 sentinel */}
  <div ref={sentinelRef} />

  {isFetchingNextPage && <스켈레톤 카드 3개 />}
  {isError && <p>게시글을 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.</p>}
  {!hasNextPage && posts.length > 0 && <p>모든 게시글을 불러왔습니다.</p>}
  {!isLoading && !isError && posts.length === 0 && <p>아직 게시글이 없습니다.</p>}

  <Link href={ROUTES.board.freeWrite}>글쓰기</Link>
</section>
```

**Intersection Observer 설정:**

```ts
const sentinelRef = useRef<HTMLDivElement>(null);

useEffect(() => {
  const observer = new IntersectionObserver(
    ([entry]) => { if (entry.isIntersecting && hasNextPage) fetchNextPage(); },
    { threshold: 0.1 }
  );
  if (sentinelRef.current) observer.observe(sentinelRef.current);
  return () => observer.disconnect();
}, [hasNextPage, fetchNextPage]);
```

- `usePostList({ type: "FREE", sort, authorMbti, targetMbti })` — `page` 파라미터 없음
- 필터/정렬 변경 시 쿼리 키가 바뀌어 자동으로 첫 페이지부터 재로딩
- 초기 로딩: 스켈레톤 카드 5개 (`isLoading === true` 시)

### 7-2. 작성 페이지 (`app/(main)/board/free/write/page.tsx`)

**클라이언트 컴포넌트** (미들웨어가 비로그인 시 `/login`으로 리다이렉트)  
단, 비회원도 작성 가능하므로 **로그인 여부와 무관하게** 폼 노출 — 미들웨어 `matcher`에서 `/board/free/write` 제외 필요.

> **미들웨어 수정 필요**: `middleware.ts`의 `PROTECTED_PREFIXES`에서 `ROUTES.board.freeWrite` 제거.  
> 비로그인 시 `guestPassword` 필드를 조건부 노출하는 방식으로 처리.

```
상태:
  title: string
  content: string (Tiptap JSON)
  targetMbti: string ("ALL" 기본)
  guestPassword: string   // 비로그인 시만 사용
  showGuestField: boolean // accessToken 없으면 true
```

렌더링 구조:

```
<form onSubmit={handleSubmit}>
  <input name="title" maxLength={150} />
  <select name="targetMbti">전체, INFP, ...</select>
  <RichContentEditor onChange={setContent} />
  {showGuestField && (
    <input name="guestPassword" type="password" placeholder="비회원 비밀번호 (4~72자)" />
  )}
  <button type="submit" disabled={isPending}>등록</button>
</form>
```

- `createPost` mutate 성공 시 → `router.push(ROUTES.board.freePost(data.id))`
- 제목 빈값, 내용 빈값, 비회원 비밀번호 길이 부족 → 클라이언트 검증 후 submit 차단

### 7-3. 상세 페이지 (`app/(main)/board/free/[id]/page.tsx`)

**클라이언트 컴포넌트**

```
// Next.js 15: params는 Promise — await 필수 (프로젝트 login/page.tsx와 동일 패턴)
const { id: idParam } = await params;
const id = Number(idParam);
if (!Number.isInteger(id) || id <= 0) notFound();
```

렌더링 구조:

```
<article>
  <header>
    <h1>{post.title}</h1>
    <MbtiBadge mbti={post.authorMbtiId} verified={post.mbtiVerified} />
    <span>{post.authorNickname}</span>
    <time>{post.createdAt}</time>
    <span>조회 {post.viewCount}</span>
  </header>
  <section>
    <RichContentViewer content={post.content} />
  </section>
  <footer>
    {/* 회원 본인 글: isOwner = true, isGuest = false */}
    {post.isOwner && !post.isGuest && (
      <>
        <button onClick={handleEditClick}>수정</button>
        <button onClick={handleDeleteClick}>삭제</button>
      </>
    )}
    {/* 비회원 글: isOwner = false (백엔드 고정). 비밀번호 아는 사람만 처리 가능 */}
    {post.isGuest && (
      <>
        <button onClick={handleEditClick}>수정 (비회원)</button>
        <button onClick={handleDeleteClick}>삭제 (비회원)</button>
      </>
    )}
  </footer>
</article>
<GuestPasswordModal ... />
```

**수정 흐름 (인라인 수정, 별도 edit 라우트 없음):**

```
상태: isEditing: boolean (default: false)
```

> 백엔드 설계: 비회원 글의 `isOwner`는 항상 `false`. 비회원 수정/삭제는 `isGuest = true` 조건으로 버튼 노출 후 비밀번호 검증으로 처리.

- 클릭 "수정" → `isEditing = true` → 제목 `<input>` + `RichContentEditor`(initialContent 주입)로 전환, `RichContentViewer` 숨김
- "저장" 클릭 시:
  - `!isGuest` (회원 본인 글) → `PATCH` 바로 호출 (guestPassword 없음), 저장 중 버튼 `disabled`
  - `isGuest` (비회원 글) → `GuestPasswordModal(action="update")` 열기 → 비밀번호 입력 → `PATCH` (제목+내용+비밀번호 동시 전송)
- 성공 시 → `isEditing = false`, `usePostDetail` 쿼리 무효화로 최신 내용 리렌더링
- "취소" 클릭 시 → `isEditing = false`, 변경사항 버림

**삭제 흐름:**

- `!isGuest` (회원 본인 글) → `window.confirm` 확인 다이얼로그 → `DELETE` → 성공 시 `router.push(ROUTES.board.free)`
- `isGuest` (비회원 글) → `GuestPasswordModal(action="delete")` 열기 → 비밀번호 입력 → `DELETE` (X-Guest-Password 헤더) → 성공 시 `router.push(ROUTES.board.free)`

**404 처리:**

- `usePostDetail` 에러가 404(POST_NOT_FOUND)이면 Next.js `notFound()` 호출 → 404 페이지로 전환

---

## 8. 미들웨어 수정

현재 `middleware.ts`는 `/board/*/write`를 인증 필수 경로로 보호 중이나, FREE 게시판은 비회원 작성이 가능하다.

| 경로                  | 변경 전                | 변경 후                 |
| --------------------- | ---------------------- | ----------------------- |
| `/board/free/write`   | 로그인 필수 리다이렉트 | 보호 해제 (비회원 가능) |
| `/board/vote/write`   | 로그인 필수            | 유지 (투표는 회원 전용) |
| `/board/debate/write` | 로그인 필수            | 유지 (토론은 회원 전용) |

`PROTECTED_PREFIXES`에서 `ROUTES.board.freeWrite` 항목 제거.

---

## 9. 주요 주의사항

- **content 직렬화**: `editor.getJSON()` 결과를 `JSON.stringify()`해서 서버로 전송. 뷰어에서는 `JSON.parse(content)`로 복원 후 `setContent()`.
- **authorNickname**: 백엔드가 탈퇴 사용자에 대해 `"(탈퇴한 사용자)"` 치환 후 반환. 프론트에서 null 체크 불필요.
- **isOwner 판단**: 백엔드가 JWT를 보고 반환하므로 프론트에서 별도 UUID 비교 불필요.
- **targetMbtiId null**: "전체 대상"을 의미. 필터 드롭다운에서 `ALL` 선택 시 쿼리 파라미터 생략.
- **페이지 리셋**: 필터/정렬 변경 시 `params` 객체가 바뀌어 쿼리 키가 교체됨 → `useInfiniteQuery`가 자동으로 page 0부터 재시작. 별도 상태 초기화 불필요.
- **Tiptap SSR**: `RichContentEditor`, `RichContentViewer`는 `dynamic(() => import(...), { ssr: false })`로 동적 로드. Tiptap은 브라우저 DOM에 의존하므로 SSR 불가.

---

## 10. 보안 체크리스트 (Frontend)

- [ ] XSS — `RichContentViewer`는 Tiptap 내부 파서만 사용. `dangerouslySetInnerHTML` 사용 금지
- [ ] 비회원 비밀번호 — 클라이언트 전송 시 평문 (HTTPS 전제). state에서 사용 후 폼 초기화, 로컬 저장 금지
- [ ] 소유권 UI — 회원 글: `isOwner && !isGuest` / 비회원 글: `isGuest` 조건으로 버튼 분기
- [ ] 작성 폼 클라이언트 검증 — title 1~150자, content 비어있으면 submit 차단
- [ ] 게스트 비밀번호 길이 검증 — 4~72자 클라이언트 검증 (백엔드 @Size와 동일 조건)
- [ ] `sort` 파라미터 대문자 — 백엔드로 `LATEST`/`VIEWS`/`COMMENTS` 전송 (소문자 전송 시 400)
- [ ] `params.id` 유효성 검증 — `parseInt("1abc")` = 1 (NaN 아님)이므로 `parseInt` 사용 금지. `Number(params.id)`로 파싱 후 `Number.isInteger(id) && id > 0` 이 false이면 `notFound()` 호출
- [ ] `JSON.parse(content)` try-catch — `RichContentViewer`에서 파싱 실패 시 에러 메시지 렌더링, throw 차단
- [ ] Open Redirect 수정 (두 경로 모두) —
  - **직접 로그인**: `LoginForm.tsx:37` `router.push(redirectTo)` → `safeRedirect` 사용
  - **OAuth2 로그인**: `LoginForm.tsx` Google/Kakao 버튼 onClick에서 `sessionStorage.setItem(OAUTH2_REDIRECT_KEY, redirectTo)` 저장 전에도 동일 검증 필요. `OAuth2CallbackContent.tsx:31` `router.replace(redirectTo)` 에서도 방어적으로 재검증.
  - 검증 조건: `redirectTo.startsWith("/") && !redirectTo.startsWith("//")`; 실패 시 `"/"` 폴백
- [ ] AT 만료 시 `isOwner` 스탈 — `JwtAuthenticationFilter`는 만료 AT를 401 없이 통과시키므로 `GET /api/posts/{id}` 는 anonymous로 처리 → `isOwner: false` 반환. 401 인터셉터가 트리거되지 않아 silent refresh 미발생. 대응: `useAuthStore` AT 변경 시(silent refresh 성공 후) 상세 페이지 `usePostDetail` 쿼리 무효화 (`queryClient.invalidateQueries(["posts", id])`)
- [ ] 이중 제출 방지 — 수정 저장 버튼도 `isPending` 시 `disabled` 처리 (작성 폼과 동일)
- [ ] `authorNickname` 타입 수정 — 백엔드 양쪽 DTO(`PostSummaryResponse`, `PostDetailResponse`) 모두 null 대신 `"(탈퇴한 사용자)"` 반환. `string | null` → `string`으로 교체, 프론트 null 치환 코드 제거
- [ ] 비회원 비밀번호 브루트포스 (백엔드 필요) — `isGuest` 글 수정/삭제 버튼이 전체 공개되므로 임의 사용자가 반복 시도 가능. BCrypt 지연(~100ms)만으로는 자동화 공격 방어 불충분. 백엔드에 IP 기반 Rate Limiting 적용 요청 필요 (프론트 단독 방어 불가)
