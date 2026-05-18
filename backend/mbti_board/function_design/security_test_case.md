# Security 테스트 케이스 목록

## 1단계 — 로컬 회원가입 · 로그인

| ID | 케이스 | 기대 결과 |
|---|---|---|
| 1-1 | 정상 정보로 회원가입 | 201 Created |
| 1-2 | 이미 존재하는 이메일로 회원가입 | 409 Conflict, "이미 사용 중인 이메일입니다." |
| 1-3 | 이미 존재하는 닉네임으로 회원가입 | 409 Conflict, "이미 사용 중인 닉네임입니다." |
| 1-4 | 정상 이메일·비밀번호로 로그인 | 200 OK + accessToken body + refreshToken HttpOnly Cookie |
| 1-5 | 올바르지 않은 비밀번호로 로그인 | 401 Unauthorized, "이메일 또는 비밀번호가 올바르지 않습니다." |

## 2단계 — 인증 필터 검증

| ID | 케이스 | 기대 결과 |
|---|---|---|
| 2-1 | Authorization 헤더 없이 인증 필요 엔드포인트 요청 | 401 Unauthorized, "인증이 필요합니다." |
| 2-2 | 유효한 AT로 인증 필요 엔드포인트 요청 | 200 OK |
| 2-3 | 서명이 변조된 AT로 인증 필요 엔드포인트 요청 | 401 Unauthorized, "토큰이 만료되었습니다." |

## 3단계 — 로그아웃 + 블랙리스트

| ID | 케이스 | 기대 결과 |
|---|---|---|
| 3-1 | 유효한 AT로 로그아웃 | 200 OK + Redis BL 키 생성 확인 + RT 키 삭제 확인 |
| 3-2 | 로그아웃 후 동일 AT로 재요청 | 401 Unauthorized, "토큰이 만료되었습니다." |

## 4단계 — 토큰 재발급 (RT Rotation)

| ID | 케이스 | 기대 결과 |
|---|---|---|
| 4-1 | 유효한 RT Cookie로 재발급 요청 | 200 OK + 새 AT body + 새 RT Set-Cookie + Redis RT 교체 확인 |
| 4-2 | RT Cookie 없이 재발급 요청 | 401 Unauthorized, "Refresh Token 쿠키가 없습니다." |
| 4-3 | 재발급 후 구 RT로 재요청 (Reuse Detection) | 401 Unauthorized, "토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다." + 전체 RT 삭제 확인 |

---

<!-- 5·6단계는 OAuth Provider 리다이렉트가 필요해 자동화 불가 → 브라우저 + Postman으로 수동 테스트 -->

## 5단계 — Google OAuth *(수동 테스트)*

| ID | 케이스 | 확인 방법 | 기대 결과 |
|---|---|---|---|
| 5-1 | OAuth 리다이렉트 시작 | 브라우저: `http://localhost:8080/oauth2/authorization/google` | Google 로그인 페이지로 리다이렉트 |
| 5-2 | 콜백 및 토큰 발급 | 로그인 완료 후 콜백 URL 확인 | URL 쿼리에 `accessToken` 존재 + DevTools Cookies에 `refreshToken` 존재 |
| 5-3 | 신규 유저 DB 저장 | DB 조회 | `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'`, `social_provider = 'google'` |
| 5-4 | 동일 계정 재로그인 (중복 가입 방지) | DB 조회 | 기존 row 유지, 새 row 생성 없음 |
| 5-5 | 발급된 AT로 인증 API 호출 | Postman: `Authorization: Bearer {accessToken}` | 200 OK |

## 6단계 — Kakao OAuth *(수동 테스트)*

| ID | 케이스 | 확인 방법 | 기대 결과 |
|---|---|---|---|
| 6-1 | OAuth 리다이렉트 시작 | 브라우저: `http://localhost:8080/oauth2/authorization/kakao` | Kakao 로그인 페이지로 리다이렉트 |
| 6-2 | 콜백 및 토큰 발급 | 로그인 완료 후 콜백 URL 확인 | URL 쿼리에 `accessToken` 존재 + DevTools Cookies에 `refreshToken` 존재 |
| 6-3 | 신규 유저 DB 저장 | DB 조회 | `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'`, `social_provider = 'kakao'`, 카카오 닉네임 기반 nickname |
| 6-4 | 이메일 동의 거부 | Kakao 동의 화면에서 이메일 제공 거부 | 400 Bad Request, "이메일 제공에 동의해야 소셜 로그인이 가능합니다." |

---

## 7단계 — 엣지 케이스

| ID | 케이스 | 기대 결과 |
|---|---|---|
| 7-1 | 소셜 계정 이메일로 로컬 로그인 시도 | 400 Bad Request, "소셜 로그인 전용 계정입니다." |
| 7-2 | LOCKED 상태 계정 로그인 | 403 Forbidden, "계정이 잠겨 있습니다." |
| 7-2 | WITHDRAWN 상태 계정 로그인 | 403 Forbidden, "탈퇴 처리 중인 계정입니다." |
| 7-2 | PERMANENT_BAN 상태 계정 로그인 | 403 Forbidden, "정지된 계정입니다." |
| 7-3 | 존재하지 않는 MBTI ID로 회원가입 | 404 Not Found, "존재하지 않는 MBTI입니다." |
| 7-4 | 동일 계정 두 기기 로그인 후 기기A만 로그아웃 | 기기A RT 삭제 + 기기B RT 유지 확인 |
