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

| Method | URL | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/api/posts` | 불필요 | 게시글 목록 조회 |
| `GET` | `/api/posts/{id}` | 불필요 | 게시글 단건 조회 |
| `POST` | `/api/posts` | 선택 (비회원 가능) | 게시글 작성 |
| `PATCH` | `/api/posts/{id}` | 선택 (비회원 가능) | 게시글 수정 |
| `DELETE` | `/api/posts/{id}` | 선택 (비회원 가능) | 게시글 삭제 |

### GET /api/posts — 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 제약 | 설명 |
|---|---|---|---|---|
| `type` | String | `FREE` | - | 게시판 유형 코드 |
| `sort` | String | `latest` | - | `latest` / `comments` / `views` |
| `authorMbti` | String | (없음) | - | 작성자 MBTI 필터 |
| `targetMbti` | String | `ALL` | - | 타겟 MBTI 필터 (`ALL` = 전체) |
| `page` | int | `0` | `@Min(0)` | 페이지 번호 |
| `size` | int | `20` | `@Min(1) @Max(100)` | 페이지 크기 |

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

| 상황 | 처리 |
|---|---|
| 작성 시 `SecurityContext` 비어있음 | `user = null`, `authorMbti = NONE`, `guestPassword` BCrypt 해싱 저장 |
| 수정 시 비회원 | `X-Guest-Password` 헤더 → `PostUpdateRequest.guestPassword` 로 BCrypt 검증 |
| 삭제 시 비회원 | `X-Guest-Password` 헤더 → `PostDeleteRequest.guestPassword` 로 BCrypt 검증 |
| 수정/삭제 시 회원 | `post.getUser().getId()` vs JWT 유저 UUID 비교, 불일치 시 403 |
| 탈퇴 회원 작성 글 | `user = null`, `guestPassword = null` → 수정/삭제 불가 (403) |

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
