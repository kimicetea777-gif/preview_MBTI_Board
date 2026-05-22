# 테스트 코드 컨벤션

## 클래스 구조

- 모든 통합 테스트는 `IntegrationTestSupport`를 상속
- `IntegrationTestSupport` = `@SpringBootTest` + `@ActiveProfiles("test")` + URL 상수만
- 공통 인프라 빈(`MockMvc`, `ObjectMapper` 등)과 헬퍼 메서드는 각 테스트 클래스에 유지
  - 테스트 파일이 5개 이상으로 늘어나면 `BaseControllerTest extends IntegrationTestSupport` 중간 계층 도입 고려

## URL 상수

- 모든 API URL 상수는 `IntegrationTestSupport`에 `protected static final`로 선언
- 각 테스트 클래스에 URL 상수를 재선언하지 않음

## 테스트 격리

- 각 테스트는 독립 실행 가능해야 함 (순서 의존 금지)
- `@AfterEach tearDown()`에서 반드시 DB + Redis 초기화

```java
@AfterEach
void tearDown() {
    // FK 관계가 있으면 자식 테이블 먼저 삭제 (예: posts → users)
    postRepository.deleteAll();
    userRepository.deleteAll();
    redisTemplate.execute((RedisCallback<Void>) connection -> {
        connection.serverCommands().flushAll();
        return null;
    });
}
```

## MockBean

- `EmailService`는 항상 `@MockBean` — 실제 메일 발송 차단
- 이메일 인증 우회: `tokenRedisService.markEmailVerified(email)` 직접 호출

## 헬퍼 메서드

- 반복되는 사전 조건(회원가입·로그인·게시글 작성 등)은 `private` 헬퍼로 추출
- 네이밍: `<동작>_후_<반환값>_추출` (예: `회원가입_후_로그인_AT_추출`, `비회원_게시글_작성_후_ID_추출`)

## 테스트 메서드 네이밍

- 메서드명: `<대상>_<조건>_<성공|실패>` (예: `게시글_작성_비회원_비밀번호_없음_실패`)
- `@DisplayName`: `"N-M. 케이스 설명 → 기대 결과"` 형식

```java
@Test
@DisplayName("3-3. 비회원 guestPassword 없음 → 400, '비회원은 비밀번호를 입력해야 합니다.'")
void 게시글_작성_비회원_비밀번호_없음_실패() throws Exception { ... }
```

## 도메인별 주의사항

- **Post content**: DB가 `jsonb` 타입이므로 반드시 유효한 JSON 사용
  ```java
  private static final String CONTENT = "{\"type\":\"doc\",\"content\":[]}";
  ```
- **테스트 이메일**: 도메인별로 충돌 방지 (예: `auth-test@`, `user-test@`, `post-test@`)
- **viewCount**: 단건 조회 응답은 증가 *전* 값 반환 — 다음 요청부터 반영됨
