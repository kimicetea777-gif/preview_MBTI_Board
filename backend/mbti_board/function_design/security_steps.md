# Security

### JWT + Redis Key Design

- **Access Token**:
  메모리(JS 변수)에 저장 — XSS 탈취 방어 (localStorage는 XSS에 취약하므로 비권장)
  TTL: 30분

- **Refresh Token**:
  HttpOnly Cookie에 저장 (클라이언트)
  Redis에 저장 (서버)
  Key pattern: `RT:{userId}:{deviceId}`
  Value: RefreshToken String
  TTL: 7일

- **Blacklist**:
  Key pattern: `BL:{accessToken}`
  Value: `"logout"`
  TTL: AT 남은 시간(remaining time)

---

### JWT Payload (Claims) Structure

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "deviceId": "uuid-string-for-this-session",
  "iat": 1715770000,
  "exp": 1715771800
}
```

> `role` 포함 이유: 매 요청마다 DB 권한 조회 없이 필터에서 바로 권한 판단 가능

---

### 다중 접속 허용

서버가 로그인 시 UUID를 생성하여 `deviceId`로 사용. 기기별 독립적인 RT를 Redis에 관리하여 특정 기기만 선택적으로 로그아웃 가능.

---

### 인증 흐름 (Sequence)

**로그인 (Local)**

1. ID/PW 검증
2. 서버가 `deviceId(UUID)` 생성
3. AT, RT 발급 (`deviceId`, `role` 포함)
4. Redis에 `RT:{userId}:{deviceId}` 저장
5. 클라이언트: AT → 메모리, RT → HttpOnly Cookie

**로그인 (소셜 OAuth)**

1. OAuth Provider로 리다이렉트
2. 콜백으로 Provider AccessToken 수신
3. Provider API로 이메일/프로필 조회
4. 신규 유저면 자동 가입, 기존 유저면 로그인 처리
5. 이후 Local 로그인과 동일하게 AT/RT 발급

**일반 요청**

1. 클라이언트가 AT를 Authorization 헤더에 포함하여 요청
2. 필터에서 AT 서명/만료 검증
3. Redis 블랙리스트(`BL:{accessToken}`) 존재 여부 확인
4. 정상이면 SecurityContext에 인증 정보 등록

**토큰 재발급 (AT 만료)**

1. 클라이언트가 RT(Cookie)를 서버에 전송
2. 서버가 Redis의 `RT:{userId}:{deviceId}`와 비교
3. 일치하면 AT, RT 모두 재발급
4. Redis의 기존 RT 삭제 후 새 RT 저장 (RT Rotation)
5. **RT Reuse Detection**: 이미 삭제된 RT로 재발급 요청이 오면 해당 `userId`의 모든 RT 즉시 폐기 (탈취 감지)

**로그아웃**

1. 현재 AT를 Redis 블랙리스트에 등록 (TTL: AT 남은 시간)
2. Redis에서 `RT:{userId}:{deviceId}` 삭제 (해당 기기만 로그아웃)

---

## 보안 설계 주의사항

개발 중 실제로 발견·수정된 취약점과 알려진 위험 항목. 향후 기능 추가 시 동일한 실수를 반복하지 않기 위해 기록.

---

### [취약점] permitAll 경로에 와일드카드 금지

**증상**: `/api/auth/**` 로 permitAll 설정 시 `/api/auth/logout`도 포함됨.  
비인증 요청이 logout에 도달하면 `@AuthenticationPrincipal`이 null → NPE로 500 발생.

**규칙**: 인증 관련 경로는 반드시 개별 경로를 나열.

```java
// 나쁜 예
.requestMatchers("/api/auth/**").permitAll()

// 올바른 예
.requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/reissue").permitAll()
```

---

### [취약점] OAuth2 속성 null 검증 누락

**증상**: Kakao 사용자가 이메일 동의를 거부하면 `getEmail()`이 null 반환.  
`User.email`은 `NOT NULL` 컬럼이므로 null 그대로 저장 시도 → `DataIntegrityViolationException`으로 의미 없는 500 발생.

**규칙**: OAuth2 속성을 Entity에 저장하기 전 반드시 null 검증 후 `CustomException` throw.

```java
if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
    throw new CustomException(ErrorCode.OAUTH2_EMAIL_REQUIRED);
}
```

---

### [설계 위험] OAuth2 콜백에서 AT를 URL 쿼리 파라미터로 전달

**증상**: OAuth2 성공 후 프론트엔드로 리다이렉트할 때 AT를 `?accessToken=...`으로 전달.  
URL은 서버 로그, 브라우저 히스토리, Referer 헤더에 기록될 수 있어 AT 노출 위험이 있음.

**현재 대응**: AT TTL 30분으로 짧게 유지. 프론트엔드는 콜백 진입 즉시 AT를 JS 메모리로 이동 후 `history.replaceState`로 URL에서 제거해야 함.

**향후 개선 방향**: 일회성 `code`를 URL로 전달하고 프론트엔드가 서버에 code를 제출해 AT를 응답 body로 받는 방식(Authorization Code 교환 패턴)으로 전환 가능.

---

### [설계 위험] Redis KEYS 명령 사용

**위치**: `TokenRedisService.deleteAllRefreshTokens()`

**증상**: `KEYS RT:{userId}:*`는 O(N) 전체 스캔 명령. Redis 키가 수백만 개일 경우 서버 블로킹 위험.

**현재 대응**: RT Reuse Detection(토큰 탈취 감지) 시에만 호출. 정상 흐름에서는 절대 호출되지 않음.

**향후 개선 방향**: 키 수가 증가할 경우 `SCAN` 명령으로 교체.

---

### [코드 품질] 공통 상수는 단일 위치에서 관리

**증상**: `CookieUtils`와 `AuthService` 양쪽에 `REFRESH_TOKEN_COOKIE_NAME = "refreshToken"` 상수가 중복 존재.  
한쪽만 수정하면 쿠키 이름 불일치로 RT 추출이 조용히 실패.

**규칙**: 쿠키 이름처럼 여러 곳에서 참조하는 값은 한 곳(`CookieUtils.REFRESH_TOKEN_COOKIE_NAME`)에서만 정의하고 참조.

---

### [설계] 401 원인 구분 — request attribute 패턴

**배경**: 필터에서 예외를 던지면 `GlobalExceptionHandler`가 잡지 못해 응답 포맷이 불일치.  
필터는 조용히 통과시키고 `EntryPoint`에서 일관된 JSON을 반환하는 구조 유지.

**구현**: 토큰이 있었으나 유효하지 않을 때 필터에서 사유를 저장.

```java
// JwtAuthenticationFilter
if (!authenticated) {
    request.setAttribute("jwt-error", "TOKEN_INVALID");
}

// JwtAuthenticationEntryPoint
String jwtError = (String) request.getAttribute("jwt-error");
String message = "TOKEN_INVALID".equals(jwtError)
        ? "토큰이 만료되었습니다."
        : "인증이 필요합니다.";
```

| 상황                    | 메시지                     | 프론트 동작        |
| ----------------------- | -------------------------- | ------------------ |
| AT 없음                 | `"인증이 필요합니다."`     | 로그인 페이지 이동 |
| AT 만료/변조/블랙리스트 | `"토큰이 만료되었습니다."` | reissue 시도       |

---

### [코드 품질] 보안 핸들러도 공통 응답 포맷 사용

**증상**: `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`에서 `Map.of("success", false, ...)`를 직접 사용.  
`ApiResponse` 구조가 바뀌면 핸들러 응답만 혼자 따라가지 못함.

**규칙**: 보안 핸들러도 `ApiResponse.fail()`을 사용해 응답 포맷 일관성 유지.

---

## 개발 계획 (3단계)

### 추가 의존성

`build.gradle`에 아래 1개 추가 (Security·JWT는 이미 포함):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

---

### 패키지 구조

```
kimicetea/
├── domain/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   └── dto/
│   │       ├── SignupRequest.java
│   │       ├── LoginRequest.java
│   │       └── LoginResponse.java
│   └── user/
│       ├── repository/UserRepository.java
│       └── security/
│           ├── CustomUserDetails.java
│           └── CustomUserDetailsService.java
└── global/
    └── security/
        ├── config/SecurityConfig.java
        ├── jwt/
        │   ├── JwtProvider.java
        │   └── JwtAuthenticationFilter.java
        ├── redis/TokenRedisService.java
        └── oauth2/
            ├── userinfo/OAuth2UserInfo.java       ← Provider별 추출 인터페이스
            ├── userinfo/GoogleOAuth2UserInfo.java
            ├── userinfo/KakaoOAuth2UserInfo.java
            ├── CustomOAuth2UserService.java
            └── OAuth2AuthenticationSuccessHandler.java

```

---

### 구현 순서

**Step 1 — 설정**

1. `build.gradle` — `oauth2-client` 추가
2. `application.yaml` — JWT secret/expiry, OAuth2 provider 클라이언트 정보 추가

**Step 2 — JWT 핵심** 3. `JwtProvider` — AT/RT 생성·검증·claims 추출 4. `TokenRedisService` — RT 저장/삭제, BL 등록/확인, Reuse Detection

**Step 3 — Spring Security 기반** 5. `UserRepository` 6. `CustomUserDetails` — `UserDetails` 구현체 (userId, email, role, deviceId 보관) 7. `CustomUserDetailsService` — `UserDetailsService` 구현 (로컬 로그인용 DB 조회) 8. `JwtAuthenticationFilter` — `OncePerRequestFilter`: AT 검증 → BL 확인 → SecurityContext 등록

**Step 4 — 소셜 OAuth2** 9. `OAuth2UserInfo` — Provider별 정보 추출 인터페이스 (`getEmail()`, `getProviderId()`, `getNickname()`) 10. `GoogleOAuth2UserInfo`, `KakaoOAuth2UserInfo` — 구현체 11. `CustomOAuth2UserService` — `DefaultOAuth2UserService` 확장: `social_provider` + `provider_id`로 신규/기존 판단 후 자동 가입 12. `OAuth2AuthenticationSuccessHandler` — 콜백 성공 후 AT/RT 발급, AT는 redirect URL 쿼리 파라미터로 전달

**Step 5 — 필터 체인 통합** 13. `SecurityConfig` — `JwtAuthenticationFilter` 등록, OAuth2 로그인 설정, permitAll/authenticated 경로 설정

**Step 6 — 로컬 인증 API** 14. DTO: `SignupRequest`, `LoginRequest`, `LoginResponse` 15. `AuthService` — signup, login, reissue, logout 16. `AuthController`

---

### API 엔드포인트

| Method | Path                               | 설명                                | 인증                    |
| ------ | ---------------------------------- | ----------------------------------- | ----------------------- |
| POST   | `/api/auth/signup`                 | 로컬 회원가입                       | 없음                    |
| POST   | `/api/auth/login`                  | 로컬 로그인 → AT(body) + RT(Cookie) | 없음                    |
| POST   | `/api/auth/reissue`                | 토큰 재발급 (RT Rotation)           | RT Cookie               |
| POST   | `/api/auth/logout`                 | 로그아웃 (AT BL 등록 + RT 삭제)     | AT                      |
| GET    | `/oauth2/authorization/{provider}` | OAuth 리다이렉트                    | 없음 (Spring 자동 처리) |
| GET    | `/login/oauth2/code/{provider}`    | OAuth 콜백                          | 없음 (Spring 자동 처리) |

---

### OAuth2 신규/기존 유저 판단 기준

`users.social_provider` + `users.provider_id` 조합으로 기존 회원 여부 확인.

- **신규**: 자동 가입 (`state=NORMAL`, `role=USER`, `mbtiVerified=DEFAULT`) → AT/RT 발급
- **기존**: 바로 AT/RT 발급

**Provider별 이메일·ID 추출 경로**

| Provider | email                                  | providerId          |
| -------- | -------------------------------------- | ------------------- |
| Google   | `attributes["email"]`                  | `attributes["sub"]` |
| Kakao    | `attributes["kakao_account"]["email"]` | `attributes["id"]`  |

---

> 테스트 계획 및 케이스는 `security_test_design.md` 참조

