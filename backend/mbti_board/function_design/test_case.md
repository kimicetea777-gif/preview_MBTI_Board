# Security 테스트 케이스 목록

## 9단계 — 이메일 인증 플로우

| ID  | 케이스                           | 엔드포인트                            | 기대 결과                                             |
| --- | -------------------------------- | ------------------------------------- | ----------------------------------------------------- |
| 9-1 | 이메일 인증 발송                 | `POST /api/auth/email-verify/send`    | 200 OK, Redis `EV:TOKEN:{uuid}` 키 생성 (TTL 10분)    |
| 9-2 | 유효한 토큰으로 인증 확인        | `POST /api/auth/email-verify/confirm` | 200 OK, `EV:DONE:{email}` 키 생성·`EV:TOKEN` 키 삭제  |
| 9-3 | 유효하지 않은 토큰으로 인증 시도 | `POST /api/auth/email-verify/confirm` | 400 Bad Request, "유효하지 않은 인증 링크입니다."     |
| 9-4 | 이메일 인증 없이 회원가입        | `POST /api/auth/signup`               | 400 Bad Request, "이메일 인증이 완료되지 않았습니다." |
| 9-5 | 이메일 인증 후 회원가입 성공     | `POST /api/auth/signup`               | 201 Created, `EV:DONE` 키 삭제 확인                   |

---

## 1단계 — 로컬 회원가입 · 로그인

| ID  | 케이스                            | 기대 결과                                                     |
| --- | --------------------------------- | ------------------------------------------------------------- |
| 1-1 | 정상 정보로 회원가입              | 201 Created                                                   |
| 1-2 | 이미 존재하는 이메일로 회원가입   | 409 Conflict, "이미 사용 중인 이메일입니다."                  |
| 1-3 | 이미 존재하는 닉네임으로 회원가입 | 409 Conflict, "이미 사용 중인 닉네임입니다."                  |
| 1-4 | 정상 이메일·비밀번호로 로그인     | 200 OK + accessToken body + refreshToken HttpOnly Cookie      |
| 1-5 | 올바르지 않은 비밀번호로 로그인   | 401 Unauthorized, "이메일 또는 비밀번호가 올바르지 않습니다." |

## 2단계 — 인증 필터 검증

| ID  | 케이스                                            | 기대 결과                                  |
| --- | ------------------------------------------------- | ------------------------------------------ |
| 2-1 | Authorization 헤더 없이 인증 필요 엔드포인트 요청 | 401 Unauthorized, "인증이 필요합니다."     |
| 2-2 | 유효한 AT로 인증 필요 엔드포인트 요청             | 200 OK                                     |
| 2-3 | 서명이 변조된 AT로 인증 필요 엔드포인트 요청      | 401 Unauthorized, "토큰이 만료되었습니다." |

## 3단계 — 로그아웃 + 블랙리스트

| ID  | 케이스                       | 기대 결과                                        |
| --- | ---------------------------- | ------------------------------------------------ |
| 3-1 | 유효한 AT로 로그아웃         | 200 OK + Redis BL 키 생성 확인 + RT 키 삭제 확인 |
| 3-2 | 로그아웃 후 동일 AT로 재요청 | 401 Unauthorized, "토큰이 만료되었습니다."       |

## 4단계 — 토큰 재발급 (RT Rotation)

| ID  | 케이스                                     | 기대 결과                                                                                           |
| --- | ------------------------------------------ | --------------------------------------------------------------------------------------------------- |
| 4-1 | 유효한 RT Cookie로 재발급 요청             | 200 OK + 새 AT body + 새 RT Set-Cookie + Redis RT 교체 확인                                         |
| 4-2 | RT Cookie 없이 재발급 요청                 | 401 Unauthorized, "Refresh Token 쿠키가 없습니다."                                                  |
| 4-3 | 재발급 후 구 RT로 재요청 (Reuse Detection) | 401 Unauthorized, "토큰 재사용이 감지되었습니다. 모든 기기에서 로그아웃됩니다." + 전체 RT 삭제 확인 |

---

<!-- 5·6단계는 OAuth Provider 리다이렉트가 필요해 자동화 불가 → 브라우저 + Postman으로 수동 테스트 -->

## 5단계 — Google OAuth _(수동 테스트)_

| ID  | 케이스                              | 확인 방법                                                     | 기대 결과                                                                     |
| --- | ----------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| 5-1 | OAuth 리다이렉트 시작               | 브라우저: `http://localhost:8080/oauth2/authorization/google` | Google 로그인 페이지로 리다이렉트                                             |
| 5-2 | 콜백 및 토큰 발급                   | 로그인 완료 후 콜백 URL 확인                                  | URL 쿼리에 `accessToken` 존재 + DevTools Cookies에 `refreshToken` 존재        |
| 5-3 | 신규 유저 DB 저장                   | DB 조회                                                       | `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'`, `social_provider = 'google'` |
| 5-4 | 동일 계정 재로그인 (중복 가입 방지) | DB 조회                                                       | 기존 row 유지, 새 row 생성 없음                                               |
| 5-5 | 발급된 AT로 인증 API 호출           | Postman: `Authorization: Bearer {accessToken}`                | 200 OK                                                                        |

## 6단계 — Kakao OAuth _(수동 테스트)_

| ID  | 케이스                | 확인 방법                                                    | 기대 결과                                                                                                 |
| --- | --------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------- |
| 6-1 | OAuth 리다이렉트 시작 | 브라우저: `http://localhost:8080/oauth2/authorization/kakao` | Kakao 로그인 페이지로 리다이렉트                                                                          |
| 6-2 | 콜백 및 토큰 발급     | 로그인 완료 후 콜백 URL 확인                                 | URL 쿼리에 `accessToken` 존재 + DevTools Cookies에 `refreshToken` 존재                                    |
| 6-3 | 신규 유저 DB 저장     | DB 조회                                                      | `mbti_id = 'NONE'`, `mbti_verified = 'DEFAULT'`, `social_provider = 'kakao'`, 카카오 닉네임 기반 nickname |
| 6-4 | 이메일 동의 거부      | Kakao 동의 화면에서 이메일 제공 거부                         | 400 Bad Request, "이메일 제공에 동의해야 소셜 로그인이 가능합니다."                                       |

---

## 8단계 — 유저 도메인

| ID   | 케이스                              | 엔드포인트                             | 기대 결과                                                                                                    |
| ---- | ----------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| 8-1  | 인증된 유저 내 프로필 조회          | `GET /api/users/me`                    | 200 OK, email·role·state·isSocialOnly 포함                                                                   |
| 8-2  | AT 없이 내 프로필 요청              | `GET /api/users/me`                    | 401 Unauthorized, "인증이 필요합니다."                                                                       |
| 8-3  | 닉네임 변경                         | `PATCH /api/users/me`                  | 200 OK, 변경된 닉네임 반환                                                                                   |
| 8-4  | MBTI 변경                           | `PATCH /api/users/me`                  | 200 OK, mbtiId 반영·mbtiVerified = SELF                                                                      |
| 8-5  | 이미 사용 중인 닉네임으로 변경      | `PATCH /api/users/me`                  | 409 Conflict, "이미 사용 중인 닉네임입니다."                                                                 |
| 8-6  | 존재하지 않는 MBTI ID로 변경        | `PATCH /api/users/me`                  | 404 Not Found, "존재하지 않는 MBTI입니다."                                                                   |
| 8-7  | 비인증 상태로 공개 프로필 조회      | `GET /api/users/{id}`                  | 200 OK, email·state 필드 없음                                                                                |
| 8-8  | 존재하지 않는 유저 공개 프로필 조회 | `GET /api/users/{id}`                  | 404 Not Found, "존재하지 않는 사용자입니다."                                                                 |
| 8-9  | 탈퇴 OTP 발송                       | `POST /api/users/me/withdrawal-verify` | 200 OK, Redis WD:{userId} 키 생성·6자리 코드                                                                 |
| 8-10 | OTP 검증 후 탈퇴 성공               | `DELETE /api/users/me`                 | 200 OK + PII 익명화 + Redis RT 삭제 + AT 블랙리스트 등록                                                     |
| 8-11 | 탈퇴 후 동일 AT 재사용              | `GET /api/users/me`                    | 401 Unauthorized, "토큰이 만료되었습니다."                                                                   |
| 8-12 | 비밀번호 변경                       | `PATCH /api/users/me/password`         | 200 OK, "비밀번호가 변경되었습니다."                                                                         |
| 8-13 | 현재 비밀번호 불일치                | `PATCH /api/users/me/password`         | 400 Bad Request, "현재 비밀번호가 올바르지 않습니다."                                                        |
| 8-14 | 소셜 전용 계정 비밀번호 변경 시도   | `PATCH /api/users/me/password`         | 400 Bad Request, "소셜 로그인 전용 계정입니다."                                                              |
| 8-15 | 잘못된 OTP 코드로 탈퇴 (1~2회)      | `DELETE /api/users/me`                 | 400 Bad Request, "인증 코드가 올바르지 않습니다."                                                            |
| 8-16 | OTP 발급 없이 탈퇴 시도             | `DELETE /api/users/me`                 | 400 Bad Request, "인증 코드가 만료되었습니다. 다시 요청해 주세요."                                           |
| 8-17 | OTP 3회 오답 → OTP 무효화           | `DELETE /api/users/me`                 | 400 Bad Request, "인증 코드를 3회 잘못 입력하였습니다. 새로운 인증 코드를 요청해 주세요." + OTP 키 삭제 확인 |

---

## 7단계 — 엣지 케이스

| ID  | 케이스                                       | 기대 결과                                       |
| --- | -------------------------------------------- | ----------------------------------------------- |
| 7-1 | 소셜 계정 이메일로 로컬 로그인 시도          | 400 Bad Request, "소셜 로그인 전용 계정입니다." |
| 7-2 | LOCKED 상태 계정 로그인                      | 403 Forbidden, "계정이 잠겨 있습니다."          |
| 7-2 | WITHDRAWN 상태 계정 로그인                   | 403 Forbidden, "탈퇴 처리 중인 계정입니다."     |
| 7-2 | PERMANENT_BAN 상태 계정 로그인               | 403 Forbidden, "정지된 계정입니다."             |
| 7-3 | 존재하지 않는 MBTI ID로 회원가입             | 404 Not Found, "존재하지 않는 MBTI입니다."      |
| 7-4 | 동일 계정 두 기기 로그인 후 기기A만 로그아웃 | 기기A RT 삭제 + 기기B RT 유지 확인              |

---

## Phase 2 — 게시글 도메인 (`PostControllerTest`)

### 목록 조회 (`GET /api/posts`)

| ID  | 케이스                        | 기대 결과                                   |
| --- | ----------------------------- | ------------------------------------------- |
| 1-1 | 게시글 없을 때 목록 조회      | 200 OK, 빈 `content` 배열                   |
| 1-2 | 게시글 작성 후 목록 조회      | 200 OK, `totalElements=1`                   |
| 1-3 | `targetMbti` 필터 — 조건 불일치 | 200 OK, `totalElements=0`                 |
| 1-4 | `page=-1`                     | 400 Bad Request                             |
| 1-5 | `size=0`                      | 400 Bad Request                             |
| 1-6 | `size=101`                    | 400 Bad Request                             |

### 단건 조회 (`GET /api/posts/{id}`)

| ID  | 케이스                          | 기대 결과                                                       |
| --- | ------------------------------- | --------------------------------------------------------------- |
| 2-1 | 정상 조회 + viewCount 증가 확인 | 200 OK, 1차 `viewCount=0` → 2차 `viewCount=1`                   |
| 2-2 | 존재하지 않는 ID 조회           | 404 Not Found, "게시글을 찾을 수 없습니다."                     |
| 2-3 | 회원이 본인 글 조회             | 200 OK, `isOwner=true`, `isGuest=false`                         |
| 2-4 | 비회원 글 조회                  | 200 OK, `isGuest=true`, `isOwner=false`                         |

### 게시글 작성 (`POST /api/posts`)

| ID  | 케이스                         | 기대 결과                                               |
| --- | ------------------------------ | ------------------------------------------------------- |
| 3-1 | 회원 작성 성공                 | 201 Created, `isOwner=true`, `isGuest=false`            |
| 3-2 | 비회원 작성 성공               | 201 Created, `isGuest=true`, `authorMbtiId=NONE`        |
| 3-3 | 비회원 — `guestPassword` 없음  | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다." |
| 3-4 | 존재하지 않는 `type`           | 400 Bad Request, "존재하지 않는 게시판 유형입니다."     |
| 3-5 | `title` 공백                   | 400 Bad Request                                         |
| 3-6 | `targetMbti` 특정 MBTI 지정    | 201 Created, `targetMbtiId` 반환                        |
| 3-7 | `targetMbti=ALL`               | 201 Created, `targetMbtiId=null`                        |

### 게시글 수정 (`PATCH /api/posts/{id}`)

| ID  | 케이스                         | 기대 결과                                               |
| --- | ------------------------------ | ------------------------------------------------------- |
| 4-1 | 회원 본인 수정 성공            | 200 OK, 변경된 `title` 반환                             |
| 4-2 | 다른 회원이 수정               | 403 Forbidden, "게시글 수정/삭제 권한이 없습니다."      |
| 4-3 | 비회원 — 올바른 비밀번호       | 200 OK, 변경된 `title` 반환                             |
| 4-4 | 비회원 — 틀린 비밀번호         | 403 Forbidden, "비밀번호가 일치하지 않습니다."          |
| 4-5 | 비회원 — 비밀번호 없음         | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다." |
| 4-6 | 존재하지 않는 게시글           | 404 Not Found, "게시글을 찾을 수 없습니다."             |

### 게시글 삭제 (`DELETE /api/posts/{id}`)

| ID  | 케이스                                   | 기대 결과                                               |
| --- | ---------------------------------------- | ------------------------------------------------------- |
| 5-1 | 회원 본인 삭제 성공                      | 200 OK, 이후 단건 조회 → 404 (소프트 삭제 확인)         |
| 5-2 | 다른 회원이 삭제                         | 403 Forbidden, "게시글 수정/삭제 권한이 없습니다."      |
| 5-3 | 비회원 — 올바른 `X-Guest-Password`       | 200 OK, 이후 단건 조회 → 404                            |
| 5-4 | 비회원 — 틀린 `X-Guest-Password`         | 403 Forbidden, "비밀번호가 일치하지 않습니다."          |
| 5-5 | 비회원 — `X-Guest-Password` 헤더 없음   | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다." |
| 5-6 | 존재하지 않는 게시글                     | 404 Not Found, "게시글을 찾을 수 없습니다."             |

---

## Phase 3 — 댓글 도메인 (`CommentControllerTest`, `CommentReactionControllerTest`)

### 댓글 목록 조회 (`GET /api/posts/{postId}/comments`)

| ID  | 케이스                                           | 기대 결과                                                                  |
| --- | ------------------------------------------------ | -------------------------------------------------------------------------- |
| 1-1 | 댓글 없는 게시글 조회                           | 200 OK, 빈 배열                                                            |
| 1-2 | 루트 댓글만 있을 때 조회                        | 200 OK, `replies = []`                                                     |
| 1-3 | 루트 + 대댓글 구조 조회                         | 200 OK, 루트의 `replies`에 대댓글 포함, 대댓글의 `replies = []`            |
| 1-4 | 비인증 유저 조회                                | 200 OK, 모든 댓글 `myReactionType = null`                                  |
| 1-5 | 리액션 있는 회원 조회                           | 200 OK, 본인이 리액션한 댓글만 `myReactionType = 1` 또는 `-1`              |
| 1-6 | 리액션 없는 회원 조회                           | 200 OK, 모든 댓글 `myReactionType = null`                                  |
| 1-7 | 소프트 삭제된 댓글 (`is_visible=false`) 포함 여부 | 200 OK, 삭제된 댓글 응답에 미포함                                          |
| 1-8 | 존재하지 않는 게시글                            | 404 Not Found, "게시글을 찾을 수 없습니다."                                |

### 댓글 작성 (`POST /api/posts/{postId}/comments`)

| ID   | 케이스                                          | 기대 결과                                                              |
| ---- | ----------------------------------------------- | ---------------------------------------------------------------------- |
| 2-1  | 회원 루트 댓글 작성                             | 201 Created, `isGuest=false`, `authorMbtiId` = 회원 MBTI              |
| 2-2  | 비회원 댓글 작성                                | 201 Created, `isGuest=true`, `authorMbtiId=NONE`                       |
| 2-3  | 비회원 — `guestPassword` 없음                   | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다."                |
| 2-4  | 대댓글 작성 (유효한 `parentId`)                 | 201 Created, 목록 조회 시 루트의 `replies`에 포함                      |
| 2-5  | 대댓글의 대댓글 시도 (`parentId`가 대댓글 ID)   | 400 Bad Request, "대댓글에는 댓글을 달 수 없습니다."                   |
| 2-6  | `parentId`가 다른 게시글 소속 댓글 ID           | 404 Not Found, "댓글을 찾을 수 없습니다."                              |
| 2-7  | 존재하지 않는 게시글에 작성                     | 404 Not Found, "게시글을 찾을 수 없습니다."                            |
| 2-8  | `content` 공백                                  | 400 Bad Request                                                        |
| 2-9  | DEBATE 게시글 — `debateStance` 없이 작성        | 400 Bad Request, "토론 게시글 댓글은 찬반 입장(1 또는 -1)을 선택해야 합니다." |
| 2-10 | DEBATE 게시글 — `debateStance=1`로 작성         | 201 Created                                                            |
| 2-11 | FREE 게시글 — `debateStance` 포함 작성          | 400 Bad Request, "일반·투표 게시글 댓글에는 찬반 입장을 설정할 수 없습니다." |

### 댓글 수정 (`PATCH /api/posts/{postId}/comments/{id}`)

| ID  | 케이스                                   | 기대 결과                                               |
| --- | ---------------------------------------- | ------------------------------------------------------- |
| 3-1 | 회원 본인 수정                           | 200 OK, 변경된 `content` 반환                           |
| 3-2 | 다른 회원이 수정 시도                    | 403 Forbidden, "댓글 수정/삭제 권한이 없습니다."        |
| 3-3 | 비회원 — 올바른 `guestPassword`          | 200 OK, 변경된 `content` 반환                           |
| 3-4 | 비회원 — 틀린 `guestPassword`            | 403 Forbidden, "비밀번호가 일치하지 않습니다."          |
| 3-5 | 비회원 — `guestPassword` 없음            | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다." |
| 3-6 | 존재하지 않는 댓글 수정                  | 404 Not Found, "댓글을 찾을 수 없습니다."               |
| 3-7 | 다른 게시글의 댓글 ID로 접근             | 404 Not Found, "댓글을 찾을 수 없습니다."               |

### 댓글 삭제 (`DELETE /api/posts/{postId}/comments/{id}`)

| ID  | 케이스                                     | 기대 결과                                               |
| --- | ------------------------------------------ | ------------------------------------------------------- |
| 4-1 | 회원 본인 삭제                             | 200 OK, 이후 목록 조회 시 해당 댓글 미포함              |
| 4-2 | 다른 회원이 삭제 시도                      | 403 Forbidden, "댓글 수정/삭제 권한이 없습니다."        |
| 4-3 | 비회원 — 올바른 `X-Guest-Password`         | 200 OK, 이후 목록 조회 시 해당 댓글 미포함              |
| 4-4 | 비회원 — 틀린 `X-Guest-Password`           | 403 Forbidden, "비밀번호가 일치하지 않습니다."          |
| 4-5 | 비회원 — `X-Guest-Password` 헤더 없음      | 400 Bad Request, "비회원은 비밀번호를 입력해야 합니다." |
| 4-6 | 존재하지 않는 댓글 삭제                    | 404 Not Found, "댓글을 찾을 수 없습니다."               |
| 4-7 | 다른 게시글의 댓글 ID로 접근               | 404 Not Found, "댓글을 찾을 수 없습니다."               |

### 리액션 토글 (`POST /api/comments/{commentId}/reactions`)

| ID   | 케이스                              | 기대 결과                                                              |
| ---- | ----------------------------------- | ---------------------------------------------------------------------- |
| 5-1  | 좋아요 신규 등록                    | 200 OK, `myReactionType=1`, `likeCount+1`                              |
| 5-2  | 싫어요 신규 등록                    | 200 OK, `myReactionType=-1`, `dislikeCount+1`                          |
| 5-3  | 좋아요 → 같은 타입 재등록 (취소)    | 200 OK, `myReactionType=null`, `likeCount` 원복                        |
| 5-4  | 싫어요 → 같은 타입 재등록 (취소)    | 200 OK, `myReactionType=null`, `dislikeCount` 원복                     |
| 5-5  | 좋아요 → 싫어요 전환                | 200 OK, `myReactionType=-1`, `likeCount-1`, `dislikeCount+1`           |
| 5-6  | 싫어요 → 좋아요 전환                | 200 OK, `myReactionType=1`, `dislikeCount-1`, `likeCount+1`            |
| 5-7  | 비인증 접근                         | 401 Unauthorized                                                       |
| 5-8  | 존재하지 않는 댓글                  | 404 Not Found, "댓글을 찾을 수 없습니다."                              |
| 5-9  | `reactionType=0` (유효하지 않은 값) | 400 Bad Request, "리액션 타입은 1(좋아요) 또는 -1(싫어요)이어야 합니다." |
| 5-10 | `reactionType` 없음 (null)          | 400 Bad Request                                                        |

### 리액션 취소 (`DELETE /api/comments/{commentId}/reactions`)

| ID  | 케이스                          | 기대 결과                                        |
| --- | ------------------------------- | ------------------------------------------------ |
| 6-1 | 좋아요 있을 때 취소             | 200 OK, `myReactionType=null`, `likeCount-1`     |
| 6-2 | 싫어요 있을 때 취소             | 200 OK, `myReactionType=null`, `dislikeCount-1`  |
| 6-3 | 리액션 없을 때 취소 (멱등)      | 200 OK, `myReactionType=null`                    |
| 6-4 | 비인증 접근                     | 401 Unauthorized                                 |
| 6-5 | 존재하지 않는 댓글              | 404 Not Found, "댓글을 찾을 수 없습니다."        |

---

## Phase 4 — 투표 게시판 (`VoteControllerTest`)

### 투표 게시글 작성 (`POST /api/posts` with `type=VOTE`)

| ID   | 케이스                                       | 기대 결과                                                                |
| ---- | -------------------------------------------- | ------------------------------------------------------------------------ |
| 1-1  | 선택지 2개로 정상 작성                       | 201 Created, `options` 배열 포함 응답                                    |
| 1-2  | 선택지 16개로 작성 (최대 — MBTI 모드 대응)   | 201 Created                                                              |
| 1-3  | 선택지 17개로 작성 (최대 초과)               | 400 Bad Request                                                          |
| 1-4  | `voteOptions` 없음                           | 400 Bad Request, "투표 게시글은 최소 2개 이상의 선택지가 필요합니다."   |
| 1-5  | 선택지 1개 (최소 미달)                       | 400 Bad Request, "투표 게시글은 최소 2개 이상의 선택지가 필요합니다."   |
| 1-6  | `endsAt`이 과거 시각                         | 400 Bad Request, "마감일은 현재 시각보다 이후여야 합니다."               |
| 1-7  | MBTI 선택지 + 일반 선택지 혼합               | 400 Bad Request, "MBTI 선택지와 일반 선택지를 혼합할 수 없습니다."      |
| 1-8  | MBTI 선택지 중 동일 MBTI 중복               | 400 Bad Request, "같은 투표에 동일한 MBTI 선택지를 중복 등록할 수 없습니다." |
| 1-9  | `endsAt` 없음 (무기한 투표)                  | 201 Created, `endsAt=null`, `isEnded=false`                              |

### 투표 참여 (`POST /api/posts/{postId}/vote`)

| ID   | 케이스                                       | 기대 결과                                                                |
| ---- | -------------------------------------------- | ------------------------------------------------------------------------ |
| 2-1  | 회원 정상 투표                               | 200 OK, `myVotedOptionId` 반환, 해당 선택지 `voteCount+1`               |
| 2-2  | 비인증 접근                                  | 401 Unauthorized                                                         |
| 2-3  | 이미 투표 후 다른 선택지로 변경              | 200 OK, 기존 선택지 `voteCount-1` · 새 선택지 `voteCount+1`             |
| 2-4  | 이미 투표 후 같은 선택지 재선택              | 200 OK, `voteCount` 변화 없음, `myVotedOptionId` 동일                    |
| 2-5  | 마감된 투표에 참여                           | 400 Bad Request, "이미 마감된 투표입니다."                               |
| 2-6  | 존재하지 않는 게시글 ID                      | 404 Not Found, "게시글을 찾을 수 없습니다."                              |
| 2-7  | FREE 게시글에 투표 요청 (vote_posts 미존재)  | 404 Not Found, "투표 게시글을 찾을 수 없습니다."                         |
| 2-8  | 존재하지 않는 선택지 ID                      | 404 Not Found, "투표 선택지를 찾을 수 없습니다."                         |
| 2-9  | 다른 게시글 소속 선택지 ID 사용              | 400 Bad Request, "선택지가 해당 투표 게시글에 속하지 않습니다."          |

### 투표 결과 조회 (`GET /api/posts/{postId}/vote/result`)

| ID   | 케이스                                       | 기대 결과                                                                |
| ---- | -------------------------------------------- | ------------------------------------------------------------------------ |
| 3-1  | 비인증 조회                                  | 200 OK, `myVotedOptionId=null`                                           |
| 3-2  | 인증했지만 미투표 상태 조회                  | 200 OK, `myVotedOptionId=null`                                           |
| 3-3  | 투표 후 결과 조회                            | 200 OK, `myVotedOptionId` 반환, 해당 선택지 `voteCount` 반영             |
| 3-4  | 마감된 투표 결과 조회                        | 200 OK, `isEnded=true`                                                   |
| 3-5  | 존재하지 않는 게시글 ID                      | 404 Not Found, "게시글을 찾을 수 없습니다."                              |
