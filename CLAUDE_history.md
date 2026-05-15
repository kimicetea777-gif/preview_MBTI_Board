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
