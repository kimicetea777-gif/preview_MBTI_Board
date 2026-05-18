# Security 테스트 설계

## 도구

| 도구                                                     | 용도                                                            |
| -------------------------------------------------------- | --------------------------------------------------------------- |
| Swagger UI `http://localhost:8080/swagger-ui/index.html` | 로컬 인증 API 수동 테스트                                       |
| Postman                                                  | RT Cookie 포함 요청 (reissue) — Swagger는 Cookie 자동 전송 불가 |
| 브라우저                                                 | OAuth 흐름 테스트 (쿠키 자동 전송)                              |
| Redis CLI `redis-cli`                                    | Redis 키 상태 직접 확인                                         |
| MockMvc + `@SpringBootTest`                              | 자동화 통합 테스트                                              |

> **Swagger 주의**: RT Cookie가 필요한 `/reissue`는 Swagger 테스트 불가. Postman 사용.

---

## 자동화 테스트 구조

```
test/
├── domain/auth/
│   └── AuthControllerTest.java   ← MockMvc 통합 테스트 (1~4단계, 7단계)
└── global/security/
    ├── JwtProviderTest.java       ← 단위 테스트
    └── TokenRedisServiceTest.java ← 단위 테스트 (embedded-redis)
```

> OAuth(5·6단계)는 Provider 리다이렉트가 필요해 자동화 불가 → 수동 테스트만

---

## 1단계 — 로컬 회원가입 · 로그인

**1-1. 회원가입 성공** `POST /api/auth/signup`

```json
{
  "email": "test@example.com",
  "password": "password123!",
  "nickname": "테스터",
  "mbtiId": "INTJ"
}
```

- 기대 응답: `201 Created`

**Redis 확인**

```bash
redis-cli keys "RT:*"
# RT:{userId}:{deviceId} 키 생성 확인
```

**1-2. 중복 이메일 회원가입** `POST /api/auth/signup`

동일 이메일로 재요청

- 기대 응답: `409 Conflict`
- 기대 메시지: `"이미 사용 중인 이메일입니다."`

**1-3. 중복 닉네임 회원가입** `POST /api/auth/signup`

동일 닉네임, 다른 이메일로 요청

- 기대 응답: `409 Conflict`
- 기대 메시지: `"이미 사용 중인 닉네임입니다."`

**1-4. 로그인 성공** `POST /api/auth/login`

```json
{ "email": "test@example.com", "password": "password123!" }
```

- 기대 응답: `200 OK`
- 응답 body: `accessToken`, `email`, `nickname`, `role` 포함 확인
- 응답 헤더 `Set-Cookie`: `refreshToken=...; HttpOnly; Path=/api/auth` 확인

**1-5. 잘못된 비밀번호 로그인** `POST /api/auth/login`

```json
{ "email": "test@example.com", "password": "wrongpassword" }
```

- 기대 응답: `401 Unauthorized`
- 기대 메시지: `"이메일 또는 비밀번호가 올바르지 않습니다."`

---

## 2단계 — 인증 필터 검증

**2-1. AT 없이 인증 필요 엔드포인트 요청**

`POST /api/auth/logout` — Authorization 헤더 없이 요청

- 기대 응답: `401 Unauthorized`
- 기대 메시지: `"인증이 필요합니다."` ← AT 자체가 없으므로

**2-2. 유효한 AT로 요청**

`Authorization: Bearer {accessToken}`

- 기대 응답: `200 OK`

**2-3. 변조된 AT 요청**

`Authorization: Bearer invalidtoken`

- 기대 응답: `401 Unauthorized`
- 기대 메시지: `"토큰이 만료되었습니다."` ← AT가 있었지만 유효하지 않으므로

---

## 3단계 — 로그아웃 + 블랙리스트

**3-1. 로그아웃** `POST /api/auth/logout`

- `Authorization: Bearer {accessToken}`
- 기대 응답: `200 OK`

**Redis 확인**

```bash
redis-cli keys "BL:*"   # 블랙리스트 키 생성 확인
redis-cli keys "RT:*"   # RT 키 삭제 확인
```

**3-2. 블랙리스트 AT 재사용**

로그아웃 후 동일 AT로 인증 요청

- 기대 응답: `401 Unauthorized`
- 기대 메시지: `"토큰이 만료되었습니다."` ← AT는 있었지만 블랙리스트 차단됨
- 참고: 블랙리스트 차단도 `TOKEN_INVALID`로 처리 (만료와 동일 분기)

---

## 4단계 — 토큰 재발급 (RT Rotation)

> Postman 사용 권장 (Cookie 수동 설정 가능)

**4-1. 정상 재발급** `POST /api/auth/reissue`

RT Cookie가 저장된 상태에서 요청

- 기대 응답: `200 OK` + 새 `accessToken` + 새 `Set-Cookie: refreshToken=...`

**Redis 확인**

```bash
redis-cli keys "RT:*"
# 새 RT로 교체 확인 (기존 RT 삭제 + 신규 RT 저장)
```

**4-2. RT Cookie 없이 재발급 요청**

Cookie 없이 `POST /api/auth/reissue`

- 기대 응답: `401 Unauthorized`
- 기대 메시지: `"인증이 필요합니다."` ← RT(Cookie)가 없으므로

**4-3. RT Reuse Detection**

재발급 이전의 구 RT를 Cookie에 직접 넣어 `/reissue` 재요청

- 기대 응답: `401 Unauthorized`
- 기대 동작: 해당 userId의 **모든 RT 즉시 삭제** (전체 기기 강제 로그아웃)

```bash
redis-cli keys "RT:{userId}:*"
# 빈 결과여야 함
```

---

## 5단계 — Google OAuth (수동 테스트)

**5-1. OAuth 리다이렉트 시작**

브라우저에서 접근: `http://localhost:8080/oauth2/authorization/google`

- Google 로그인 페이지 리다이렉트 확인

**5-2. 콜백 및 토큰 발급**

로그인 완료 후: `http://localhost:3000/oauth2/callback?accessToken={token}`

- URL 쿼리 파라미터에서 `accessToken` 확인
- DevTools → Application → Cookies에서 `refreshToken` 확인

**5-3. DB 신규 유저 확인**

```sql
SELECT id, email, social_provider, provider_id, mbti_id, mbti_verified
FROM users WHERE social_provider = 'google';
```

- `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'` 확인

**5-4. 기존 유저 재로그인 (중복 가입 방지)**

동일 Google 계정 재로그인 후 DB 조회

- 동일 `id`로 기존 레코드 유지 확인 (새 row 생성 X)

**5-5. 발급된 AT로 API 호출**

`Authorization: Bearer {accessToken}` 포함 후 인증 필요 엔드포인트 요청

- 기대 응답: `200 OK`

---

## 6단계 — Kakao OAuth (수동 테스트)

**6-1. OAuth 리다이렉트 시작**

브라우저에서 접근: `http://localhost:8080/oauth2/authorization/kakao`

- Kakao 로그인 페이지 리다이렉트 확인

**6-2. 콜백 및 토큰 발급**

로그인 완료 후: `http://localhost:3000/oauth2/callback?accessToken={token}`

- `accessToken`, `refreshToken` 쿠키 확인

**6-3. DB 신규 유저 확인**

```sql
SELECT id, email, social_provider, provider_id, nickname, mbti_id, mbti_verified
FROM users WHERE social_provider = 'kakao';
```

- `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'` 확인
- `nickname`이 카카오 프로필 닉네임 기반으로 생성됐는지 확인

**6-4. 이메일 미동의 시 오류 처리**

Kakao 동의 화면에서 이메일 제공 동의 거부 후 진행

- 기대 응답: `400 Bad Request`
- 기대 메시지: `"이메일 제공에 동의해야 소셜 로그인이 가능합니다."`
- 주의: 카카오 앱 설정에서 이메일 동의 항목이 '선택'이어야 테스트 가능

---

## 7단계 — 엣지 케이스

**7-1. 소셜 계정으로 로컬 로그인 시도**

```json
{ "email": "google-user@gmail.com", "password": "anypassword" }
```

- 기대 응답: `400 Bad Request`
- 기대 메시지: `"소셜 로그인 전용 계정입니다."`

**7-2. 계정 상태별 로그인 차단**

DB에서 상태 변경 후 로그인 시도:

```sql
UPDATE users SET state = 'LOCKED'        WHERE email = 'test@example.com';
UPDATE users SET state = 'WITHDRAWN'     WHERE email = 'test@example.com';
UPDATE users SET state = 'PERMANENT_BAN' WHERE email = 'test@example.com';
```

| state           | 기대 메시지                 |
| --------------- | --------------------------- |
| `LOCKED`        | `"계정이 잠겨 있습니다."`   |
| `WITHDRAWN`     | `"탈퇴 처리된 계정입니다."` |
| `PERMANENT_BAN` | `"영구 정지된 계정입니다."` |

- 테스트 후 `NORMAL`로 복구

**7-3. 유효하지 않은 MBTI ID로 회원가입**

```json
{
  "email": "test2@example.com",
  "password": "password123!",
  "nickname": "테스터2",
  "mbtiId": "INVALID"
}
```

- 기대 응답: `404 Not Found`
- 기대 메시지: `"존재하지 않는 MBTI입니다."`

**7-4. 다중 기기 로그인**

동일 계정으로 두 번 로그인 (각각 다른 AT/RT 발급)

```bash
redis-cli keys "RT:{userId}:*"
# RT:{userId}:deviceA, RT:{userId}:deviceB 두 키 생성 확인
```

- 기기A 로그아웃 시 `RT:{userId}:deviceA`만 삭제, deviceB는 유지 확인
