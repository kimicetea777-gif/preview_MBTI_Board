# Claude 작업 이력

---

## 2026-05-15

### 1. build.gradle 정리 및 버전 수정

- Spring Boot `4.0.6` → `3.3.5` 다운그레이드
- Java `25` → `21` (LTS) 변경
- JJWT `0.11.5` → `0.12.6` 업그레이드
- 중복 의존성 `spring-boot-starter` 제거
- Flyway 의존성 추가 (`flyway-core`, `flyway-database-postgresql`)

### 2. CLAUDE.md 업데이트

- 기술 스택 버전 `Java 21 + Spring Boot 3.3.5` 로 수정
- 설계 문서 섹션에 `function_design/` 디렉토리 참조 추가

### 3. 인프라 기반 세팅 (1단계)

**docker-compose.yml** (루트 생성)
- PostgreSQL 16-alpine 컨테이너 (port 5432, volume 포함, healthcheck)
- Redis 7-alpine 컨테이너 (port 6379, volume 포함, healthcheck)

**application.yaml** 설정
- Datasource (PostgreSQL 로컬 연결)
- JPA (`ddl-auto: validate` — Flyway가 스키마 관리)
- Redis 연결
- Flyway 활성화 (`classpath:db/migration`)

**V1__init_schema.sql** (`src/main/resources/db/migration/` 생성)
- `database_schema.md` DDL 전체 반영
- 순서: 테이블 생성 → 트리거 → 인덱스
- 포함 테이블: `mbti_metadata`, `users`, `mbti_test_history`, `post_types`, `posts`, `vote_posts`, `vote_post_options`, `vote_posts_history`, `debate_posts`, `comments`, `comment_reactions`

---

## 2026-05-16

### 1. .env 환경변수 전략 확정

- `spring-dotenv` 라이브러리 제거 → Spring Boot 네이티브 `spring.config.import: optional:file:../../.env` 사용
- `.env` 파일을 루트 한 곳에서만 관리 (Docker Compose + Spring Boot 공유)
- `backend/mbti_board/.gitignore`에 `.env` 추가

### 2. JPA 엔티티 전체 작성 (2단계)

총 11개 테이블 → 엔티티 매핑 완료

| 엔티티 | 파일 위치 |
|---|---|
| `MbtiMetadata` | `domain/mbti/entity/` |
| `MbtiTestHistory` | `domain/mbti/entity/` |
| `User` | `domain/user/entity/` |
| `PostType` | `domain/post/entity/` |
| `Post` | `domain/post/entity/` |
| `VotePost` | `domain/post/entity/` |
| `VotePostOption` | `domain/post/entity/` |
| `VotePostHistory` | `domain/post/entity/` |
| `DebatePost` | `domain/post/entity/` |
| `Comment` | `domain/comment/entity/` |
| `CommentReaction` | `domain/comment/entity/` |

**주요 적용 패턴**
- `@MapsId` + `@OneToOne`: `VotePost`, `DebatePost` (posts PK 공유)
- `@GeneratedValue(UUID)`: `User` PK
- `@JdbcTypeCode(SqlTypes.JSON)`: `Post.content` (JSONB)
- `insertable=false, updatable=false`: DB 트리거 관리 컬럼 (`like_count`, `dislike_count`, `comment_count`, `vote_count`)
- `view_count`: `@Modifying` 쿼리로 원자적 증가 예정 (PostRepository 구현 시)
- `optional=false`: NOT NULL FK에 적용 (INNER JOIN 최적화)
- `@Builder.Default`: DB DEFAULT 값 있는 NOT NULL 컬럼

**Enum 클래스 생성**
- `UserRole` (ADMIN/TESTER/USER/GUEST, weight 포함)
- `UserState` (NORMAL/PENDING/INACTIVE/WITHDRAWN/LOCKED/SHADOW_BANNED/PERMANENT_BAN)
- `MbtiVerified` (TESTED/SELF/DEFAULT, colorCode 포함)

### 3. 엔티티 필드 구조 통일

모든 엔티티에 구분 주석 적용 및 필드 순서 통일:
1. 일반 컬럼 필드 (`@Column`, `@Enumerated`, 타임스탬프)
2. `/*---연관관계---*/` 구분선
3. 연관관계 필드 (`@ManyToOne`, `@OneToOne`)
4. `/*---편의 메소드---*/` 구분선
5. 편의 메소드 (`hide()`, `update()`, `changeOption()` 등)

### 4. Git 및 프로젝트 구조 결정

- **모노레포** 구조 유지 결정 (`MBTI_Board/` 루트에서 `git init`)
- 루트 `.gitignore` 보강 (Gradle, IDE, Node, OS 파일 추가)
- `/.설계/` 디렉토리 gitignore 추가
- 백엔드 `.gitignore`는 Spring 특화 항목으로 유지 (중첩 구조 병합 활용)

---

## 2026-05-17

### 1. Security 구현 완료 (3단계)

**신규 클래스**
- `global/security/TokenIssuerService` — AT·RT 발급·Redis 저장·쿠키 설정을 하나의 단위로 처리. `AuthService`와 `OAuth2AuthenticationSuccessHandler`에서 중복 코드 제거 목적으로 추출

**수정 클래스**
- `AuthService` — `issueTokenPair()` 제거, `TokenIssuerService.issue()` 위임
- `OAuth2AuthenticationSuccessHandler` — `JwtProvider`, `TokenRedisService`, `CookieUtils` 직접 의존 제거, `TokenIssuerService` 단일 의존으로 교체
- `TokenRedisService` — `deleteAllRefreshTokens()` 내 `keys != null && !keys.isEmpty()` → `CollectionUtils.isEmpty()` 교체
- `SecurityConfig` — CORS 설정 주석 처리로 추가 (프론트 개발 시작 시 주석 해제)
- `JwtAuthenticationFilter` — 토큰 유효하지 않을 때 `request.setAttribute("jwt-error", "TOKEN_INVALID")` 저장
- `JwtAuthenticationEntryPoint` — `jwt-error` 속성 기반 메시지 분기 (`"토큰이 만료되었습니다."` / `"인증이 필요합니다."`)
