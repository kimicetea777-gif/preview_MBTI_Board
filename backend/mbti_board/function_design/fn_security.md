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
