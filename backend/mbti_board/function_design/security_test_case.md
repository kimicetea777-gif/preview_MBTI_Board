# Security 테스트 케이스 목록

## 9단계 — 이메일 인증 플로우

| ID | 케이스 | 엔드포인트 | 기대 결과 |
|---|---|---|---|
| 9-1 | 이메일 인증 발송 | `POST /api/auth/email-verify/send` | 200 OK, Redis `EV:TOKEN:{uuid}` 키 생성 (TTL 10분) |
| 9-2 | 유효한 토큰으로 인증 확인 | `POST /api/auth/email-verify/confirm` | 200 OK, `EV:DONE:{email}` 키 생성·`EV:TOKEN` 키 삭제 |
| 9-3 | 유효하지 않은 토큰으로 인증 시도 | `POST /api/auth/email-verify/confirm` | 400 Bad Request, "유효하지 않은 인증 링크입니다." |
| 9-4 | 이메일 인증 없이 회원가입 | `POST /api/auth/signup` | 400 Bad Request, "이메일 인증이 완료되지 않았습니다." |
| 9-5 | 이메일 인증 후 회원가입 성공 | `POST /api/auth/signup` | 201 Created, `EV:DONE` 키 삭제 확인 |

---

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

## 8단계 — 유저 도메인

| ID | 케이스 | 엔드포인트 | 기대 결과 |
|---|---|---|---|
| 8-1 | 인증된 유저 내 프로필 조회 | `GET /api/users/me` | 200 OK, email·role·state·isSocialOnly 포함 |
| 8-2 | AT 없이 내 프로필 요청 | `GET /api/users/me` | 401 Unauthorized, "인증이 필요합니다." |
| 8-3 | 닉네임 변경 | `PATCH /api/users/me` | 200 OK, 변경된 닉네임 반환 |
| 8-4 | MBTI 변경 | `PATCH /api/users/me` | 200 OK, mbtiId 반영·mbtiVerified = SELF |
| 8-5 | 이미 사용 중인 닉네임으로 변경 | `PATCH /api/users/me` | 409 Conflict, "이미 사용 중인 닉네임입니다." |
| 8-6 | 존재하지 않는 MBTI ID로 변경 | `PATCH /api/users/me` | 404 Not Found, "존재하지 않는 MBTI입니다." |
| 8-7 | 비인증 상태로 공개 프로필 조회 | `GET /api/users/{id}` | 200 OK, email·state 필드 없음 |
| 8-8 | 존재하지 않는 유저 공개 프로필 조회 | `GET /api/users/{id}` | 404 Not Found, "존재하지 않는 사용자입니다." |
| 8-9 | 탈퇴 OTP 발송 | `POST /api/users/me/withdrawal-verify` | 200 OK, Redis WD:{userId} 키 생성·6자리 코드 |
| 8-10 | OTP 검증 후 탈퇴 성공 | `DELETE /api/users/me` | 200 OK + PII 익명화 + Redis RT 삭제 + AT 블랙리스트 등록 |
| 8-11 | 탈퇴 후 동일 AT 재사용 | `GET /api/users/me` | 401 Unauthorized, "토큰이 만료되었습니다." |
| 8-12 | 비밀번호 변경 | `PATCH /api/users/me/password` | 200 OK, "비밀번호가 변경되었습니다." |
| 8-13 | 현재 비밀번호 불일치 | `PATCH /api/users/me/password` | 400 Bad Request, "현재 비밀번호가 올바르지 않습니다." |
| 8-14 | 소셜 전용 계정 비밀번호 변경 시도 | `PATCH /api/users/me/password` | 400 Bad Request, "소셜 로그인 전용 계정입니다." |
| 8-15 | 잘못된 OTP 코드로 탈퇴 (1~2회) | `DELETE /api/users/me` | 400 Bad Request, "인증 코드가 올바르지 않습니다." |
| 8-16 | OTP 발급 없이 탈퇴 시도 | `DELETE /api/users/me` | 400 Bad Request, "인증 코드가 만료되었습니다. 다시 요청해 주세요." |
| 8-17 | OTP 3회 오답 → OTP 무효화 | `DELETE /api/users/me` | 400 Bad Request, "인증 코드를 3회 잘못 입력하였습니다. 새로운 인증 코드를 요청해 주세요." + OTP 키 삭제 확인 |

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
