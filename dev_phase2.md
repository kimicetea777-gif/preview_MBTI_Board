# Phase 2 — Post 도메인 (FREE 게시판) Backend 설계

> dev_steps.md 기준. 엔티티는 이미 완성 — Repository / DTO / Service / Controller 구현 대상.

---

## 구현 대상 파일 목록

```
domain/post/
├── repository/
│   ├── PostRepository.java          (JpaRepository + 커스텀 쿼리)
│   └── PostTypeRepository.java
├── dto/
│   ├── PostCreateRequest.java
│   ├── PostUpdateRequest.java
│   ├── PostSummaryResponse.java     (목록용)
│   └── PostDetailResponse.java      (상세용)
├── mapper/
│   └── PostMapper.java              (MapStruct)
├── service/
│   └── PostService.java
└── controller/
    └── PostController.java
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

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `type` | String | `FREE` | 게시판 유형 코드 (`FREE` / `VOTE` / `DEBATE`) |
| `sort` | String | `latest` | 정렬 (`latest` / `comments` / `views`) |
| `authorMbti` | String | (없음) | MBTI 필터 (예: `INFP`) |
| `page` | int | `0` | 페이지 번호 |
| `size` | int | `20` | 페이지 크기 |

---

## DTO 설계

### PostCreateRequest

```java
String title          // @NotBlank, @Size(max = 150)
String content        // @NotBlank (Tiptap JSON 문자열)
String type           // @NotBlank — 게시판 코드 (FREE / VOTE / DEBATE)
String guestPassword  // 비회원일 때만 필수 (nullable, 서비스에서 검증)
```

### PostUpdateRequest

```java
String title          // @NotBlank, @Size(max = 150)
String content        // @NotBlank
String guestPassword  // 비회원 수정 시 필수
```

### PostDeleteRequest

```java
String guestPassword  // 비회원 삭제 시 필수
```

### PostSummaryResponse (목록)

```java
Long id
String title
String authorNickname   // user == null 이면 "익명"
String authorMbtiId     // MbtiMetadata.id (ex: "INFP")
String authorMbtiColor  // MbtiMetadata.colorCode
String mbtiVerified     // MbtiVerified.name()
int viewCount
int commentCount
Instant createdAt
```

### PostDetailResponse (상세)

```java
// PostSummaryResponse 모든 필드 +
String content          // Tiptap JSON 문자열
boolean isOwner         // 요청자가 작성자인지 (JWT 없으면 false)
```

---

## Repository 설계

### PostRepository

```java
// viewCount 원자적 증가 — 동시성 처리 핵심
@Modifying
@Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
void increaseViewCount(@Param("id") Long id);

// 목록 조회는 QueryDSL 커스텀 구현
Page<Post> findPosts(String typeCode, String sort, String authorMbti, Pageable pageable);
```

### PostTypeRepository

```java
// code로 PostType 조회 (FREE / VOTE / DEBATE)
Optional<PostType> findByCode(String code);
```

---

## Service 설계

### PostService 메서드

```
getPosts(type, sort, authorMbti, pageable)
  → QueryDSL로 조건 조합 → Page<PostSummaryResponse> 반환

getPost(id)
  → 단건 조회 + increaseViewCount 호출 → PostDetailResponse 반환
  → isVisible = false 이면 404

createPost(request, userDetails)
  → 회원: userDetails에서 User 조회, authorMbti = user.getMbtiId()
  → 비회원: user = null, authorMbti = "NONE"(DEFAULT), guestPassword BCrypt 해싱
  → PostType은 request.type 코드로 findByCode 조회

updatePost(id, request, userDetails)
  → 회원: post.getUser().getId() == 요청자 id 확인
  → 비회원: passwordEncoder.matches(request.guestPassword, post.getGuestPassword())
  → post.update(title, content) 호출

deletePost(id, request, userDetails)
  → 본인 확인 로직 동일
  → post.hide() 호출 (소프트 삭제)
```

---

## QueryDSL 목록 조회 로직

```
QPost post = QPost.post;
BooleanBuilder builder = new BooleanBuilder();

// 게시판 유형 필터
builder.and(post.postType.code.eq(typeCode));

// MBTI 필터 (선택)
if (authorMbti != null) builder.and(post.authorMbti.id.eq(authorMbti));

// 삭제된 글 제외
builder.and(post.isVisible.isTrue());

// 정렬
OrderSpecifier<?> order = switch (sort) {
    case "comments" -> post.commentCount.desc();
    case "views"    -> post.viewCount.desc();
    default         -> post.createdAt.desc();   // latest
};
```

---

## 비회원 처리 분기

| 상황 | 처리 |
|---|---|
| 작성 시 `SecurityContext` 비어있음 | `user = null`, `authorMbti = NONE(DEFAULT)`, `guestPassword` BCrypt 해싱 |
| 수정/삭제 시 비회원 | `guestPassword` 입력 받아 `passwordEncoder.matches()` 검증 |
| 수정/삭제 시 회원 | `post.getUser().getId()` vs 현재 유저 UUID 비교, 불일치 시 403 |
| 게시글 작성자가 NULL (탈퇴 회원) | authorNickname = `"(탈퇴한 사용자)"`, 수정/삭제 불가 처리 |

---

## 주요 주의사항

- `viewCount` / `commentCount` — 엔티티에서 `insertable = false, updatable = false`. **JPA 직접 수정 금지**, `@Modifying` 쿼리로만 처리
- `content` — `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"`. DB에 JSON 문자열 그대로 저장, 검증 없음
- `PostType`은 DB 마스터 데이터 (Flyway로 초기 INSERT). 존재하지 않는 code 요청 시 `CustomException` 던질 것
- `authorMbti` — 비회원은 `NONE` MbtiMetadata FK (DB에 `NONE` 레코드 존재해야 함)
- Phase 2는 FREE 게시판만. VOTE / DEBATE 확장은 Phase 4~5에서 처리

---

## ErrorCode 추가 필요 항목

```java
POST_NOT_FOUND(404, "POST_001", "게시글을 찾을 수 없습니다.")
POST_FORBIDDEN(403, "POST_002", "게시글 수정/삭제 권한이 없습니다.")
POST_TYPE_NOT_FOUND(400, "POST_003", "존재하지 않는 게시판 유형입니다.")
GUEST_PASSWORD_REQUIRED(400, "POST_004", "비회원은 비밀번호를 입력해야 합니다.")
GUEST_PASSWORD_MISMATCH(403, "POST_005", "비밀번호가 일치하지 않습니다.")
```

---

## 구현 순서

```
1. PostTypeRepository
2. PostRepository (increaseViewCount + QueryDSL 커스텀)
3. DTO 4종 (Create/Update/Delete Request, Summary/Detail Response)
4. PostMapper (MapStruct)
5. ErrorCode 추가
6. PostService
7. PostController
```
