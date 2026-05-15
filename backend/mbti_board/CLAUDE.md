# MBTI Board - Claude Code Guide

## 프로젝트 개요

MBTI 기반 커뮤니티 플랫폼. 자유글·투표·토론 게시판을 제공하며 회원/비회원 모두 이용 가능.

## 기술 스택

### Backend

- **Core**: Java 21 + Spring Boot 3.3.5
- **Database Access**: Spring Data JPA + QueryDSL
- **Security**: Spring Security & JWT
- **Cache & Session**: Redis (Refresh Token (Multi-device) & Blacklist)
- **Documentation**: Swagger/OpenAPI 3.0
- **Build Tool**: Gradle

- **Entity & DTO Mapper lib**: MapStruct

### Database & Storage

- **Main Database**: PostgreSQL(Local)
- **In-Memory Storage**: Redis

### Infrastructure & DevOps

- **Virtualization**: Docker & Docker Compose
- **Operating System**: Ubuntu
- **Networking**: Cloudflare Tunnel
- **Monitoring**: Prometheus + Grafana

## 설계 문서

- PRD (제품 요구사항): `../../prd.md`
- DB 스키마 전체 명세: `../../database_schema.md`
- 기능별 설계: `./function_design/` (기능 개발 전 반드시 확인)
  - `fn_security.md` — JWT/Redis 인증 흐름, 키 설계

---

## JPA 엔티티 컨벤션

### PK 통일 전략

모든 엔티티의 PK 필드는 `id`로 통일하고 `@Column(name = "id")`로 명시 매핑.

```java
@Id
@Column(name = "id")
```

### 1:1 확장 테이블 (`vote_posts`, `debate_posts`)

부모(`posts`)의 PK를 공유하므로 반드시 `@MapsId` 사용.

```java
@Id
private Long id;

@MapsId
@OneToOne
@JoinColumn(name = "id")
private Post post;
```

### Enum 매핑

DB에 문자열로 저장. `@Enumerated(EnumType.STRING)` 사용.

```java
@Enumerated(EnumType.STRING)
private UserRole role;
```

적용 Enum 목록:

**MbtiVerified** — MBTI 인증 배지 색상 포함
| 값 | 의미 | 배지 색상 |
|---|---|---|
| `TESTED` | 사이트 내 검사 완료 | `#2E5C40` |
| `SELF` | 사용자 직접 입력 | `#7DB27A` |
| `DEFAULT` | 미설정 회원 / 비회원 | `#A8A29E` |

**UserRole** — 권한 가중치 포함
| 값 | 의미 | weight |
|---|---|---|
| `ADMIN` | 최고 관리자 | 100 |
| `TESTER` | 모니터링/테스트 | 50 |
| `USER` | 일반 회원 | 10 |
| `GUEST` | 가입 직후 / 비회원 기본 | 1 |

**UserState** — 계정 생명주기

- `NORMAL`: 정상
- `PENDING`: 이메일 인증 대기
- `INACTIVE`: 휴면
- `WITHDRAWN`: 탈퇴 대기
- `LOCKED`: 일시 보안 잠금
- `SHADOW_BANNED`: 쉐도우 밴 (콘텐츠 노출 제한)
- `PERMANENT_BAN`: 영구 정지

### `updated_at` 자동 갱신

DB 트리거 없이 JPA가 관리. `@UpdateTimestamp` 사용.

```java
@UpdateTimestamp
private Instant updatedAt;
```

적용 엔티티: `User`, `Post`, `Comment`, `CommentReaction`, `VotePostsHistory`

---

---

## 비즈니스 규칙

### 게시판 유형별 핵심 동작

| 유형   | code     | 특이사항                                                                               |
| ------ | -------- | -------------------------------------------------------------------------------------- |
| 자유글 | `FREE`   | 제한 없음                                                                              |
| 투표   | `VOTE`   | `vote_posts` 확장. 투표는 **회원 전용**, 1인 1표, 변경 가능. `ends_at` NULL이면 무기한 |
| 토론   | `DEBATE` | `debate_posts` 확장. 댓글에 찬성(`1`) / 반대(`-1`) 스탠스 필수. `ends_at` 마감 지원    |

### 비회원 정책

- 게시글/댓글 작성 가능. 작성자 MBTI는 `DEFAULT`(미인증) 상태로 저장
- UI에 **'미인증'** 배지 표시 (`MbtiVerified.DEFAULT`, colorCode `#A8A29E`)
- 수정/삭제 시 `guest_password`로 본인 확인 (BCrypt 검증)
- 좋아요/싫어요(`comment_reactions`) 및 투표(`vote_posts_history`)는 **회원 전용**

### 댓글 계층 정책

- 대댓글은 **1단계 깊이만 허용** (`parent_id`가 있는 댓글에는 대댓글 불가)
- 토론 게시글 댓글은 `debate_stance` 필수 (`1` 또는 `-1`), 일반 게시글은 `NULL`

### 탈퇴/삭제 처리

- 회원 탈퇴 시 게시글/댓글 작성자 정보는 `NULL`로 SET — 콘텐츠는 보존
- 게시글 삭제 시 연결된 투표/토론 메타, 댓글 전체 CASCADE 삭제
