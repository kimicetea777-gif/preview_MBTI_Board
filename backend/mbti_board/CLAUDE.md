# MBTI Board (Backend) - Claude Code Guide

이 저장소(`backend/mbti_board`)에서 작업하는 Claude Code(및 다른 개발자)를 위한 안내입니다. 프론트엔드 규칙은 `../../frontend/mbti_board/CLAUDE.md`.

## 프로젝트

MBTI 기반 커뮤니티 플랫폼. 자유글·투표·토론 게시판을 제공하며 회원/비회원 모두 이용 가능.

**왜 만드는가**: 혼자 기획부터 운영까지 해 보는 서비스. 미니PC 한 대에서 실제로 운영 중이다.
**로그인은 직접 만든 인증 서버(AS)에 위임한다.** 이 서비스는 비밀번호·이메일 인증·자체 JWT 발급을 갖지 않는다 (직접 구현했다가 통째로 버렸다 — `Document/Failure and Success.md`).

## 문서

설계·운영 문서는 저장소 밖 `../../../Document/`에 있다 (git 추적 안 됨). 폴더 지도 문서는 아직 없다.
- `Failure and Success.md` — **먼저 읽을 것.** 구조가 왜 이렇게 됐는지(AS 위임, Image Server 분리, 탈퇴 재설계)
- `build_manual.md` — 이미지 빌드 → SSH 전송 → 배포·롤백 절차
- `back_fn_design/class_descript.md` — 클래스 한 줄 요약 (**클래스를 생성하면 여기에 추가**)
- `image_upload_ownership_plan.md`, `image_async_confirm_plan.md`, `image_fix_worklog.md` — 이미지 소유권·비동기 confirm 작업 기록
- `self_test.md` — 브라우저로만 확인되는 흐름(로그인, 차단, 멀티 디바이스)의 수동 체크리스트
- `back_study_note/` — 학습 노트 (AT 발급과 Security 인증 객체, 회원 탈퇴)

작업 규칙:
- 큰 변경은 `Document/`에 계획 문서를 먼저 쓰고 그 단계대로 진행한다. 폐기한 계획도 이유와 함께 남긴다.
- 문서 파일 이름은 코드 주석이 이름만으로 인용할 수 있으므로 이동은 되지만 이름 변경은 인용하는 곳을 같이 고친다.
- DB 스키마의 기준은 `src/main/resources/db/migration/`의 Flyway SQL이다. 예전 CLAUDE.md가 가리키던 `prd.md`, `database_schema.md`, `function_design/`은 현재 존재하지 않는다.

## 스택

- Java 21 + Spring Boot 3.3.5 (Gradle). Boot 4 / Java 25로 시작했다가 LTS인 3.3.5 / 21로 내렸다 — 올리지 않는다.
- Spring Data JPA + QueryDSL, MapStruct, Lombok. 스키마는 Flyway가 관리하고 JPA는 `ddl-auto: validate`.
- 인증: Spring Security **OAuth2 Client** (AS 위임). 브라우저는 HttpOnly 불투명 세션 쿠키만 갖고, AS의 AT/RT는 서버(Redis)에만 보관한다. (`build.gradle`에 jjwt 의존성이 남아 있지만 `src/main`에서 쓰지 않는다)
- PostgreSQL 16, Redis 7 (세션·AS 토큰·Rate Limit·어드민 통계 캐시). `build.gradle`의 `redisson-spring-boot-starter`는 `src/main` 코드가 직접 부르지 않지만, 자동 설정으로 `RedisConnectionFactory`를 제공해 `RedisTemplate`의 실제 클라이언트가 된다(Lettuce 아님 — 2026-11-20 Redis 정지 시 예외 메시지로 확인. Redis 장애 시 Redisson 재시도 때문에 명령 하나가 약 15초 걸린다).
- 이미지는 별도 **Image Server**가 R2에 저장한다. 이 서비스는 `global/image/client`로 호출만 한다.
- Swagger/OpenAPI(springdoc 2.3) — **prod에서는 꺼져 있다**.
- Docker Compose + Cloudflare Tunnel, 미니PC(N100, RAM 16GB, Ubuntu). Actuator/Prometheus는 `build.gradle`에서 주석 처리돼 있다.

선택 이유:
- 가상 스레드(`spring.threads.virtual.enabled`) 사용. 블로킹 코드를 그대로 쓰되 `bootRun`에서 `-Djdk.tracePinnedThreads=short`로 pinning을 본다.
- 토큰은 유저당 한 쌍이 아니라 `authServerId:deviceId` 키로 **기기마다** 보관한다. 유저 단위로 합치면 두 기기의 동시 refresh가 AS의 RT 재사용 감지에 걸려 세션이 통째로 폐기된다.

## 명령어

`backend/mbti_board`에서 실행한다.

- `./gradlew bootRun` — 개발 서버. `../../.env`(= `MBTI_Board/.env`)를 읽어 환경변수로 주입한다.
- `./gradlew test` — 테스트. **먼저 `docker compose up -d postgres redis`** 로 `postgres:5432`, `redis:6379`가 떠 있어야 한다 (`IntegrationTestSupport`).
- `./gradlew clean build -x test` — 빌드
- 배포: 루트에서 `docker compose build backend frontend` → `Document/build_manual.md`

## 핵심 원칙 (반드시 지킬 것)

1. **인증·계정은 AS가 진실이다**: 이 서비스는 AS 계정을 지우지 않는다. 게시판에서의 "탈퇴"는 AS에서 이 서비스 연결을 해제하는 것이고, 우리는 폴링으로 감지한다 (`global/deletionsync`, `global/membershipsync`). `LOCKED`·`SHADOW_BANNED`·`PERMANENT_BAN` 같은 게시판 전용 제재는 로컬 DB에서만 관리한다.
2. **하드코딩 금지**: 튜닝 값(TTL, 쿨타임, 제한 수치)·URL·enum 문자열은 상수(`global/common/ApiConstants`, `ValidationConstants`)나 설정으로 둔다. 같은 값이 두 곳 이상에 나오면 이름 붙은 상수로 모은다.
3. **카운트는 DB 트리거가 관리한다**: `like_count`, `dislike_count`, `comment_count`, `vote_count` 같은 컬럼은 엔티티에서 `insertable=false, updatable=false`다. 서비스 코드에서 직접 증감하지 않는다. `view_count`만 `@Modifying` 쿼리로 원자적으로 올린다.
4. **본문 이미지 URL은 플레이스홀더로 저장한다**: 저장소를 옮겨도 설정값 하나로 모든 글이 새 위치를 가리키게 하기 위해서다. 이미지 수명(PENDING → `confirm` → `release`)은 Image Server가 전담하고, 이 서비스는 글 저장·수정·삭제 시점에 호출만 한다.
5. **파일은 로컬 디스크에 저장하지 않는다.** 항상 Image Server → Cloudflare R2.

## 코드 컨벤션

- 에러 처리: 모든 응답은 공통 응답 포맷(`ApiResponse<T>`)으로 감싼다. 컨트롤러에서 `try-catch`로 처리하지 말고 커스텀 예외를 던져 전역 처리기(`@ControllerAdvice`, `global/common/exception`)가 응답으로 바꾸게 한다.
- 인증·권한: 필터·핸들러는 `global/security`에 있다. 비회원 허용 여부는 아래 "비즈니스 규칙" 표를 따른다.
- 입력 검증: 외부 입력(요청 body, 쿼리)은 형식·범위를 반드시 검증한다 (`ValidationConstants`). 요청 크기는 업로드 설정(`multipart` 10MB/11MB)에 걸린다.
- 주석: 코드로 알 수 있는 건 쓰지 않는다. **왜 이렇게 했는지**(숨은 제약, 우회한 버그)만 적는다.
- 새 클래스·메서드에는 JavaDoc이나 라인 주석으로 `[역할]`과 `[주의사항]`을 적는다.
- 모든 API 컨트롤러 메서드에 `@Operation`, `@ApiResponse` 같은 OpenAPI 어노테이션을 붙인다.
- 변경 가능성이 없는 변수는 `final`. 클래스 `PascalCase`, 메서드·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`.
- 새 **클래스를 생성**하면 한 줄 요약을 `Document/back_fn_design/class_descript.md`에 추가한다: `| 클래스 | 경로 | 설명 |`.
- 의존성은 쓰는 단계에서 설치한다. 새 라이브러리를 들이기 전에 기존 것으로 되는지 먼저 본다.
- 비동기 처리·대용량 트래픽 로직은 성능 병목이 없도록 논블로킹 방식이나 가벼운 스레드/태스크를 우선 고려한다.
- 이미 검증된 쿼리는 다른 큰 작업과 동시에 "최적화" 명목으로 다시 쓰지 않는다. N+1은 QueryDSL `fetchJoin`, `@EntityGraph`로 막는다.

### 구조

`com.board.kimicetea` 아래 두 갈래다.
- `domain/` — `auth`, `comment`, `mbti`, `notice`, `post`, `user`. 각 도메인은 `controller / dto / entity / repository / service`(+ `enums`, `security`).
- `global/` — 횡단 관심사: `alert`(에러 메일), `audit`(접속 기록), `common`, `config`, `crypto`, `deletionsync`, `membershipsync`, `image`, `mail`, `ratelimit`, `security`.

### 로깅·알림

- 요청마다 8자리 ID를 MDC에 심어 로그를 요청 단위로 추적한다.
- 처리되지 않은 예외는 메일로 알리되 한 번 보낸 뒤 자동 음소거한다 (관리자 API로 다시 켠다).
- Logback 같은 Spring 컨텍스트 밖 코드는 빈 주입이 안 된다. 알림 on/off 플래그가 static인 이유다.

## JPA 엔티티 컨벤션

### PK 통일 전략

모든 엔티티의 PK 필드는 `id`로 통일하고 `@Column(name = "id")`로 명시 매핑.

```java
@Id
@Column(name = "id")
```

`User`의 PK는 `@GeneratedValue(UUID)`다.

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

### 필드 순서

1. 일반 컬럼 필드 (`@Column`, `@Enumerated`, 타임스탬프)
2. `/*---연관관계---*/` 구분선 + 연관관계 필드
3. `/*---편의 메소드---*/` 구분선 + 편의 메서드
- NOT NULL FK에는 `optional=false`(INNER JOIN), DB DEFAULT가 있는 NOT NULL 컬럼에는 `@Builder.Default`.

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

**UserRole** — 권한 가중치 포함. 로그인 직후 AS의 `app_role` 클레임과 동기화하고, 이후 토큰 refresh 주기(약 5분)마다 다시 맞춘다.
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
- `WITHDRAWN`: **탈퇴 확정** (PII 익명화·세션 폐기까지 끝난 껍데기 행. 유예 중이 아니다. 다음 04:00 배치가 행을 물리 삭제한다. AS 계정 하드 삭제를 폴링(04:10)으로 감지하면 유예 없이 바로 이 상태가 된다)
- `MEMBERSHIP_WITHDRAWAL_PENDING`: **탈퇴 유예** (AS 연결 해제 후 상태. 로그인은 막히지만 PII는 보존. 유예기간 안에 재연결하면 `NORMAL`로 복구, 지나면 04:15 배치가 `WITHDRAWN`으로 확정 익명화. 유예기간은 AS 정책 30일 + 안전 마진 2일 = 32일)
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

## DB 마이그레이션 컨벤션 (Flyway)

- 스키마는 Flyway로만 바꾼다. **이미 적용된 마이그레이션(V1 등)은 고치지 않는다** — 체크섬이 깨진다. 테이블을 지울 때도 새 버전으로 drop한다.
- FK 제약은 반드시 제약명을 명시해서 작성

  ```sql
  CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users(id)
  ```

- 마이그레이션 위치는 세 곳이다. 모두 **`db/` 아래 형제 폴더**여야 한다 (Flyway는 location을 재귀 스캔하므로 `db/migration` 하위에 두면 prod에서 제외가 안 된다 — 실제로 겪은 버그).

  | 폴더 | 내용 | prod |
  |---|---|---|
  | `db/migration` | 스키마, 필수 시드(MBTI 메타데이터·문항 등) | 포함 |
  | `db/migration-seed` | 실제 사이트에 노출해도 되는 시드(집계 데이터 등) | 포함 |
  | `db/migration-dev` | 로컬 전용 더미 유저·글·댓글 | **제외** (`application-prod.yaml`) |

- 버전 번호는 세 폴더가 공유한다 (현재 최신 V16). 새 마이그레이션은 다음 번호를 쓴다.
- 로컬 DB에서 새 마이그레이션은 `./gradlew bootRun`(`.env`의 DB 계정)으로 적용한다. 테스트 계정(`application-test.yaml`의 `mbti_user`)은 `public` 스키마에 CREATE 권한이 없어 테스트 기동으로는 적용되지 않는다(`permission denied for schema public`). 적용된 뒤에는 기본 권한으로 `mbti_user`도 새 테이블을 쓸 수 있다. bootRun이 로컬 AS(9000) 부재로 기동에 실패해도 Flyway는 그 전에 끝난다.

---

## 비즈니스 규칙

### 게시판 유형별 핵심 동작

| 유형   | code     | 특이사항                                                                               |
| ------ | -------- | -------------------------------------------------------------------------------------- |
| 자유글 | `FREE`   | 제한 없음                                                                              |
| 투표   | `VOTE`   | `vote_posts` 확장. 투표는 **회원 전용**, 1인 1표, 변경 가능. `ends_at` NULL이면 무기한. 1인 1표는 `ON CONFLICT` upsert로 처리 |
| 토론   | `DEBATE` | `debate_posts` 확장. 댓글에 찬성(`1`) / 반대(`-1`) 스탠스 필수. `ends_at` 마감 지원    |

- 투표 시점의 작성자 MBTI를 스냅샷으로 남기고 트리거로 집계해 선택지별 MBTI 분포를 보여 준다.

### 비회원 정책

- 게시글/댓글 작성 가능. 작성자 MBTI는 `DEFAULT`(미인증) 상태로 저장
- UI에 **'미인증'** 배지 표시 (`MbtiVerified.DEFAULT`, colorCode `#A8A29E`)
- 수정/삭제 시 `guest_password`로 본인 확인 (BCrypt 검증)
- 좋아요/싫어요(`comment_reactions`) 및 투표(`vote_posts_history`)는 **회원 전용**

### 댓글 계층 정책

- 대댓글은 **1단계 깊이만 허용** (`parent_id`가 있는 댓글에는 대댓글 불가)
- 토론 게시글 댓글은 `debate_stance` 필수 (`1` 또는 `-1`), 일반 게시글은 `NULL`

### 탈퇴/삭제 처리

- 회원 탈퇴 시 게시글/댓글 작성자 정보는 `NULL`로 SET(익명화) — 콘텐츠는 보존. 다른 기기 세션도 즉시 무효화한다.
- 투표 득표 수는 탈퇴 후에도 유지한다.
- 게시글 삭제 시 연결된 투표/토론 메타, 댓글 전체 CASCADE 삭제

### 법적 보존·개인정보

- 접속 기록(감사 로그)은 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`로 본 요청이 커밋된 뒤에만 별도 스레드에서 저장한다.
- 탈퇴자 법적 보존 로그의 이메일은 **AES-256으로 암호화**해 저장하고 12개월 뒤 배치로 삭제한다.
- Rate Limit은 Redis `INCR` 기반 `@RateLimit` 어노테이션. Cloudflare Tunnel 뒤에서도 실제 클라이언트 IP를 뽑고, IP는 salt를 섞어 SHA-256 해시로만 저장한다.

---

## 운영 전제

- 배포 환경 제약: RAM 16GB 미니PC에 Postgres·Redis·Spring·Next.js를 함께 올린다. 컨테이너 `mem_limit`은 backend 768m이고, JVM 힙은 `Dockerfile`의 `JAVA_OPTS="-Xmx512m"`이다. 힙을 올릴 때는 `mem_limit`도 같이 본다 (예전 문서의 `-Xmx2g`는 현재 설정과 다르다).
- **단일 인스턴스 전제다.** `@Scheduled` 작업(어드민 통계 갱신 — 현재 `admin.stats.scheduler-enabled=false`로 꺼 둠, 일일 정리 04:00·04:05, 탈퇴 폴링 04:10, 멤버십 동기화 04:15, 이미지 동기화 10초 간격, 멤버십 이벤트 재시도 10초 간격)이 프로세스 안에서 돈다. 여러 대로 늘리면 중복 실행된다.
- Cloudflare Tunnel이 TLS를 종료한다. `server.forward-headers-strategy: framework`가 없으면 OAuth2 `redirect_uri`가 `http://`로 만들어져 `redirect_uri_mismatch`가 난다.
- 어드민 대시보드 통계는 Redis에 캐시한다. 사용자가 적은 동안은 주기적 사전 집계를 꺼 두었다(`admin.stats.scheduler-enabled=false`, 2026-11-18). 갱신은 어드민의 수동 새로고침(`POST .../stats/refresh`)이고, 캐시가 비면(최초 조회·TTL 4시간 만료) 조회 요청이 직접 집계한다. 사용자가 늘면 `true`로 켜면 `admin.stats.cache-refresh-delay-ms`(2시간) 주기로 미리 집계한다. 캐시 TTL은 갱신 주기보다 길어야 한다 (`AdminStatsCachePolicy`).

## 작업 방식

- **검증하고 보고한다.** 테스트·빌드·실제 실행 중 한 일과 못 한 일을 구분해서 말한다. 실행해 보지 않은 것을 "동작한다"고 하지 않는다.
- 로그인·로그아웃·탈퇴처럼 외부 서비스(AS, Image Server)와 브라우저 쿠키가 얽힌 흐름은 자동 테스트로 확인되지 않는다. `Document/self_test.md`의 항목으로 따로 확인했는지 밝힌다.
- 보안 설정·서버 부팅 훅처럼 핫 리로드가 반영하지 못하는 변경은 서버를 재시작해서 확인한다.
- **없는 내용을 지어내지 않는다.** 비어 있는 항목은 채우지 않고 물어본다.
- **범위를 지킨다.** 요청하지 않은 리팩터링·기능 추가·파일 정리를 끼워 넣지 않는다.
- **되돌리기 어려운 작업은 먼저 묻는다.** 삭제, 덮어쓰기, 강제 푸시, 운영 DB 변경, 외부 서비스에 올리는 작업.
- 결정이 바뀌면 코드와 함께 `Document/`와 이 파일의 "현재 상태"도 갱신한다.

## 보안·비밀

- `.env`, `.env_next`는 열지 않는다. 키 이름만 필요하면 `application.yaml`의 `${...}` 자리표시자를 본다.
- 비밀 값은 `.env`(git 제외)에만 두고 문서·코드·로그·커밋에 쓰지 않는다. 테스트 코드에도 secret을 하드코딩하지 않는다.
- 운영 `.env`는 GitHub를 거치지 않고 SSH로만 전달한다.
- 공개 저장소·사이트에 들어가는 문서에는 로컬 경로, 전화번호, 개인 이메일, 내부 주소를 넣지 않는다.

## Windows 환경 주의 (이 PC에서 실제로 겪은 것)

- 한글 등 비ASCII 페이로드는 명령줄에 직접 넣지 말고 파일로 쓴 뒤 `curl --data-binary @payload.json`으로 보낸다.
- Git Bash `curl -F "file=@/c/..."`처럼 POSIX 절대경로는 안 열린다. 그 폴더로 `cd`한 뒤 파일명만 준다.
- 테스트는 `-Dfile.encoding=UTF-8`로 돈다 (`build.gradle`). 콘솔 한글이 깨지면 이 설정부터 확인한다.

## 겪은 버그 — 같은 실수 주의

1. **두 기기 동시 로그인 시 5분 뒤 둘 다 로그아웃**: AS 토큰을 유저당 한 쌍만 저장해, 동시 refresh가 RT 재사용으로 판정됐다. 키를 `authServerId:deviceId`로 나눠 해결. AS의 보안 장치가 막았다면 먼저 내 설계를 의심한다.
2. **로그아웃만 누르면 400**: 캐시해 둔 `id_token`이 백그라운드 갱신 때마다 무효가 됐다. `id_token` 의존을 없애고 RT 하나로 `/oauth2/revoke`, SSO 쿠키 정리는 `client_id` 기반 `/session/logout`으로 분리했다.
3. **Flyway가 prod에서도 dev 더미를 적용**: dev 마이그레이션을 `db/migration` 하위에 두면 재귀 스캔에 걸린다. 형제 폴더로 둔다.
4. **캐시된 detached 엔티티로 500**: `ConcurrentMapCacheManager`는 JVM 공유라 이전 테스트의 캐시가 남는다. 통합 테스트는 `IntegrationTestSupport`를 상속해 `@AfterEach`에서 캐시를 비운다 (그 클래스의 주석에서 확인한 내용).
5. **V1 수정으로 체크섬 깨짐**: 적용된 마이그레이션은 건드리지 않고 새 버전으로 고친다.
6. **로컬 DB에서 `bootRun`이 `Migration checksum mismatch for migration version 12`로 실패 (2026-11-17)**: 로컬 DB를 커밋 전 초안 V12로 만든 뒤(2026-09-14) 파일의 **주석만** 고쳐 커밋했다(09-21). Flyway 체크섬은 주석도 포함한다. 같은 일을 막으려면 마이그레이션은 **커밋한 뒤에** DB에 적용하고, 적용한 뒤에는 주석 한 줄도 고치지 않는다. 이미 어긋났다면 (1) 파일 체크섬을 Flyway 방식(줄 단위 CRC32)으로 계산해 "Resolved locally" 값과 맞는지 확인하고, (2) DB의 실제 객체(컬럼·트리거·함수 본문)가 파일과 같은지 비교하고, (3) 같을 때만 `flyway repair`(또는 `flyway_schema_history.checksum` 갱신)로 맞춘다. 객체가 다르면 repair 대신 DB를 다시 만든다. 운영 DB는 같은 확인 없이 repair하지 않는다.
7. **AS 탈퇴 이벤트(CLIENT_WITHDRAWN)가 항상 실패 (2026-11-20 발견)**: V1의 `users.state VARCHAR(20)`에 `MEMBERSHIP_WITHDRAWAL_PENDING`(29자)이 들어가지 않아 `value too long`. CHECK 제약에는 값이 있었지만 핸들러 테스트가 Mockito라 아무도 몰랐고, 옛 폴러는 첫 탈퇴 이벤트에서 영구히 멈췄다. V16에서 30으로 늘리고 `UserStateColumnTest`가 enum과 실제 컬럼을 비교한다. enum 값을 추가할 때는 DB 컬럼 길이와 CHECK 제약도 같이 본다.

## 현재 상태

<마지막 갱신일 2026-11-20>
- 진행: v1.0 운영 중(2026-08~). 이미지 소유권 추적·비동기 confirm, 탈퇴/삭제 폴링(AS `hasNext`) 반영까지 커밋됨. 멤버십 이벤트 폴링 실패 재시도(`Document/membership_event_retry_plan.md`)는 구현·로컬 수동 확인 후 develop에 병합하고 운영에 배포(2026-11-20). 배포 후 첫 04:15 실행 결과는 아직 확인 전. 기능 추가 계속.
- 테스트: `src/test`에 테스트 클래스 52개(2026-11-20 `./gradlew test` 기준 464개 테스트). 통합 테스트는 postgres·redis가 떠 있어야 한다.
- 마이그레이션: 최신 V16 (`widen_users_state`). V15 `create_membership_event_failures`.
- 알려진 문제·보류 사항: `build.gradle`의 jjwt 의존성은 쓰이지 않음(제거 보류). 백엔드 `test_case.md`, `CLAUDE_test.md`는 존재하지 않음.
- 배포: 미니PC에 Docker Compose. 절차는 `Document/build_manual.md`.

이 절은 짧게 유지한다. 단계별 상세 이력은 `Document/`로 옮기고 여기엔 지금 알아야 할 것만 남긴다.
