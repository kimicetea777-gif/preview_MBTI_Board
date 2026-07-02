# Admin Stats 캐싱 전환 작업 메모

## 배경

`AdminUserStatsService`에 적용한 패턴(DB 집계 → Redis 캐시, 스케줄러 10분 갱신)을
게시글·댓글·MBTI 검사 통계에도 동일하게 적용한다.

**적용 전:** HTTP 요청마다 다수의 DB 집계 쿼리 실행 → 데이터 증가 시 응답 지연
**적용 후:** HTTP 요청 → Redis 즉시 반환 / 스케줄러가 백그라운드에서 10분마다 갱신

---

## 공통 패턴 (AdminUserStatsService 참고)

각 도메인마다 다음 3가지 변경이 동일하게 적용된다.

### 1. Stats Service 수정

```
변경 전: getStats()           — 요청 시 DB 직접 집계
변경 후: getCachedStats()     — Redis 읽기, 미스 시 computeAndCache() 폴백
         computeAndCache()    — @Transactional(readOnly=true), DB 집계 + Redis 저장
```

- 클래스 레벨 `@Transactional(readOnly = true)` 제거
- `getStats()` → `computeAndCache()` 로 이름 변경 + `@Transactional(readOnly = true)` 직접 부착
- `getCachedStats()` 신규 추가 (트랜잭션 없음)
- `StringRedisTemplate`, `ObjectMapper` 의존성 추가
- Redis TTL 1시간 (스케줄러 장애 시 안전망)

### 2. Stats Scheduler 신규 생성

```java
@Scheduled(fixedDelay = 600_000, initialDelay = 5_000)   // 10분 갱신, 기동 5초 후 선적재
public void refresh() { ... }

@Async   // 어드민 수동 새로고침 — HTTP 응답과 분리된 스레드
public void triggerAsync() { ... }
```

- 위치: 해당 도메인 `service` 패키지
- `@Async`는 외부 빈(Controller)에서 호출 → Spring 프록시 정상 적용

### 3. Stats Controller 수정

```
GET  /api/admin/{domain}/stats         → getCachedStats()   (기존)
POST /api/admin/{domain}/stats/refresh → triggerAsync()     (신규, 202 반환)
```

---

## 도메인별 작업 목록

### 게시글 (Post)

| 구분 | 파일 | 변경 내용 |
|---|---|---|
| 수정 | `domain/post/service/AdminPostStatsService.java` | `getStats()` → `computeAndCache()` + `getCachedStats()` 추가, Redis key: `ADMIN:POST:STATS` |
| 신규 | `domain/post/service/AdminPostStatsScheduler.java` | `@Scheduled` + `@Async triggerAsync()` |
| 수정 | `domain/post/controller/AdminPostController.java` | `getCachedStats()` 호출로 교체, `POST /stats/refresh` 엔드포인트 추가 |
| 수정 | `function_design/class_descript.md` | AdminPostStatsService 설명 업데이트, AdminPostStatsScheduler 추가 |

### 댓글 (Comment)

| 구분 | 파일 | 변경 내용 |
|---|---|---|
| 수정 | `domain/comment/service/AdminCommentStatsService.java` | `getStats()` → `computeAndCache()` + `getCachedStats()` 추가, Redis key: `ADMIN:COMMENT:STATS` |
| 신규 | `domain/comment/service/AdminCommentStatsScheduler.java` | `@Scheduled` + `@Async triggerAsync()` |
| 수정 | `domain/comment/controller/AdminCommentController.java` | `getCachedStats()` 호출로 교체, `POST /stats/refresh` 엔드포인트 추가 |
| 수정 | `function_design/class_descript.md` | AdminCommentStatsService 설명 업데이트, AdminCommentStatsScheduler 추가 |

### MBTI 검사 (MbtiTest)

| 구분 | 파일 | 변경 내용 |
|---|---|---|
| 수정 | `domain/mbti/service/AdminMbtiTestStatsService.java` | `getStats()` → `computeAndCache()` + `getCachedStats()` 추가, Redis key: `ADMIN:MBTI:STATS` |
| 신규 | `domain/mbti/service/AdminMbtiTestStatsScheduler.java` | `@Scheduled` + `@Async triggerAsync()` |
| 수정 | `domain/mbti/controller/AdminMbtiController.java` | `getCachedStats()` 호출로 교체, `POST /stats/refresh` 엔드포인트 추가 |
| 수정 | `function_design/class_descript.md` | AdminMbtiTestStatsService 설명 업데이트, AdminMbtiTestStatsScheduler 추가 |

---

## Redis 키 규칙

```
ADMIN:USER:STATS      ← 완료
ADMIN:POST:STATS      ← 완료
ADMIN:COMMENT:STATS   ← 완료
ADMIN:MBTI:STATS      ← 완료
```

---

## 주의사항

- `computeAndCache()` 내부에서 `@Transactional(readOnly = true)` 정상 적용되려면
  **반드시 스케줄러(외부 빈)에서 호출**해야 함 — 자기 호출(self-invocation) 금지
- `getCachedStats()` 폴백(Redis 미스) 시 자기 호출로 `@Transactional` 미적용되지만
  최초 기동 직후 단 1회만 발생하므로 허용 (각 Repository가 자체 트랜잭션 보유)
- 직렬화 대상 Response DTO가 Jackson record 역직렬화를 지원하는지 확인
  (Spring Boot 3.x + Jackson 2.15+ 환경에서는 별도 설정 불필요)
