# 개발 단계 로드맵

Security & JWT 완료 기준으로 작성. 각 Phase는 이전 Phase에 의존하므로 순서를 지킨다.
백엔드 → 프론트 순서로 Phase 단위 수직 슬라이스로 개발한다.

---

## 현재 완료 상태

- [x] 엔티티 전체 (User, Post, VotePost, DebatePost, Comment, CommentReaction, MbtiMetadata, MbtiTestHistory 등)
- [x] Security Config (JWT, OAuth2, Redis Blacklist/Refresh Token)
- [x] Auth API (회원가입, 로그인, 로그아웃, 토큰 재발급)
- [x] 전역 예외 처리 (`GlobalExceptionHandler`, `ErrorCode`, `CustomException`)
- [x] 공통 응답 포맷 (`ApiResponse<T>`)

---

## Phase 0 — 프론트 기반 세팅 (백엔드 무관, 최우선) <완료>

> 이후 모든 Phase의 프론트 작업이 이 기반 위에 올라가므로 가장 먼저 세팅한다.

### 0-1. 프로젝트 초기화 <완료>

- Next.js 14 App Router + TypeScript + Tailwind CSS 세팅
- 폴더 구조 확립: `app/`, `components/`, `lib/`, `hooks/`, `types/`

### 0-2. API 클라이언트 & 토큰 관리 <완료>

- axios 인스턴스 생성 (`lib/axios.ts`)
- Request 인터셉터: `Authorization: Bearer <accessToken>` 헤더 자동 삽입
- Response 인터셉터: 401 발생 시 `POST /api/auth/refresh` 호출 후 원 요청 재시도
- 토큰 저장: accessToken은 메모리(변수), refreshToken은 httpOnly 쿠키 (서버가 Set-Cookie)

### 0-3. TanStack Query 세팅 <완료>

- `QueryClientProvider` 루트에 등록

### 0-4. 공통 타입 정의 <완료>

- `types/api.ts` — `ApiResponse<T>` 등 백엔드 공통 응답 타입
- `types/user.ts`, `types/post.ts`, `types/comment.ts` 등 도메인 타입

### 0-5. 공통 컴포넌트 <완료>

- `MbtiBadge` — MBTI + 인증 상태(colorCode) 배지
- `ApiErrorBoundary` — 공통 에러 화면
- 레이아웃: 헤더(로그인/로그아웃), 사이드바(게시판 유형 탭)

---

## Phase 1 — User 도메인 <완료>

> 게시글/댓글 작성 전 사용자 정보 조회·수정 기반이 필요하므로 가장 먼저 개발.

### [Backend] <완료>

#### 1-1. 내 프로필 조회 / 수정 <완료>

- `GET /api/users/me` — 토큰으로 본인 정보 조회
- `PATCH /api/users/me` — 닉네임, 프로필 이미지, MBTI 수정
  - MBTI 수정 시 `mbti_verified = SELF`로 변경
  - 닉네임 앞뒤 공백 자동 제거 (strip)
- `GET /api/users/{id}` — 다른 유저 프로필 공개 조회

#### 1-2. 계정 탈퇴 <완료>

- `DELETE /api/users/me` — 상태를 `WITHDRAWN`으로 변경 (즉시 삭제 X)
  - 게시글/댓글 `user_id` → NULL 처리는 DB ON DELETE SET NULL이 담당
  - 탈퇴 즉시 전체 기기 RT 삭제 + 현재 AT 블랙리스트 등록 + RT 쿠키 만료

#### 1-3. 비밀번호 변경 (소셜 로그인 사용자는 스킵) <완료>

- `PATCH /api/users/me/password`

### [Frontend]

#### 1-4. 마이페이지 (`/my`) <완료>

- 프로필 조회 (`useQuery` — `GET /api/users/me`)
- 닉네임·MBTI 수정 폼 (`useMutation` — `PATCH /api/users/me`)
- MBTI 변경 시 `mbti_verified` 뱃지 즉시 반영

#### 1-5. 로그인 / 회원가입 페이지 (`/login`, `/signup`) <완료>

- Phase 0에서 만든 axios 인터셉터와 연동
- 소셜 로그인 버튼 (Google, Kakao)

#### 1-6. 유저 공개 프로필 (`/users/[id]`) <나중에>

- 다른 유저 정보 조회

---

## Phase 2 — Post 도메인 (FREE 게시판)

> 가장 단순한 자유글 게시판을 먼저 구현해 Post CRUD 공통 흐름을 확립한다.

### [Backend] <완료>

#### 2-1. 게시글 목록 조회 <완료>

- `GET /api/posts?type=FREE&page=0&size=20`
- 정렬: 최신순 / 댓글많은순 / 조회많은순
- MBTI 필터: `?authorMbti=INFP`
- QueryDSL 사용, 페이징은 Spring Data `Pageable`

#### 2-2. 게시글 단건 조회 <완료>

- `GET /api/posts/{id}`
- 조회 시 `view_count + 1` (동시성 이슈 최소화: DB UPDATE 단건으로 처리)

#### 2-3. 게시글 작성 <완료>

- `POST /api/posts`
- 회원: JWT 필수, `user_id` + `author_mbti`(현재 MBTI 스냅샷) 저장
- 비회원: `guest_password` 필수 (BCrypt 해싱 후 저장)
- `content`는 JSONB — 프론트 Tiptap 에디터 JSON 구조 그대로 수신

#### 2-4. 게시글 수정 <완료>

- `PATCH /api/posts/{id}`
- 회원: JWT 본인 확인
- 비회원: `guest_password` 검증

#### 2-5. 게시글 삭제 <완료>

- `DELETE /api/posts/{id}`
- 회원/비회원 본인 확인 후 소프트 삭제(`is_visible = false`)

### [Frontend]

#### 2-6. 게시판 목록 페이지 (`/board/free`) <완료>

- `useInfiniteQuery` 또는 페이지네이션 (`GET /api/posts?type=FREE`)
- MBTI 필터 드롭다운, 정렬 탭
- 게시글 카드: 제목·작성자 MBTI 뱃지·댓글수·조회수

#### 2-7. 게시글 작성 페이지 (`/board/free/write`) <완료>

- Tiptap 에디터 (`RichContentEditor`)
- 비회원일 경우 `guest_password` 입력 필드 노출

#### 2-8. 게시글 상세 페이지 (`/board/free/[id]`) <완료>

- 본문 렌더링 (Tiptap read-only)
- 수정/삭제 버튼 (본인 글만 노출)

---

## Phase 3 — Comment 도메인

> Post 단건 조회에 댓글이 필요하므로 Phase 2 직후 개발.

### [Backend]

#### 3-1. 댓글 목록 조회 <완료>

- `GET /api/posts/{postId}/comments`
- 계층형 (루트 댓글 + 대댓글 1단계) — `idx_comments_hierarchy` 인덱스 활용

#### 3-2. 댓글 작성 <완료>

- `POST /api/posts/{postId}/comments`
- 대댓글: `parent_id` 포함 — `parent_id`의 댓글에 이미 `parent_id`가 있으면 400
- 비회원: `guest_password` 필수

#### 3-3. 댓글 수정 / 삭제 <완료>

- `PATCH /api/posts/{postId}/comments/{id}`
- `DELETE /api/posts/{postId}/comments/{id}` — `is_visible = false`

#### 3-4. 댓글 리액션 (좋아요 / 싫어요) <완료>

- `POST /api/comments/{id}/reactions` — 회원 전용
- `DELETE /api/comments/{id}/reactions` — 취소
- 이미 같은 타입이면 취소, 다른 타입이면 전환 (UPDATE)
- DB 트리거가 `like_count` / `dislike_count` 자동 동기화

### [Frontend]

#### 3-5. 댓글 섹션 컴포넌트 (`CommentSection`) <완료>

- 게시글 상세 페이지 하단에 마운트
- `useQuery` — `GET /api/posts/{postId}/comments`
- 계층형 렌더링: 루트 댓글 → 대댓글 들여쓰기

#### 3-6. 댓글 작성 폼 <완료>

- 로그인 유저: 바로 작성
- 비회원: `guest_password` 입력 모달

#### 3-7. 리액션 버튼 <완료>

- 좋아요/싫어요 토글 (`useMutation`)
- 낙관적 업데이트 (TanStack Query `optimisticUpdate`)

---

## Phase 4 — Vote 게시판

> FREE 게시판 CRUD 흐름 위에 투표 메타 + 히스토리 추가.

### [Backend]

#### 4-1. 투표 게시글 작성 <완료>

- `POST /api/posts` — `type: VOTE` + `voteOptions[]` 포함
- `vote_posts` + `vote_post_options` 함께 저장 (트랜잭션)
- MBTI 투표: `mbti_target` FK 연결, 같은 게시글 내 MBTI 중복 불가

#### 4-2. 투표 참여 <완료>

- `POST /api/posts/{postId}/vote` — 회원 전용
- `vote_posts_history` UPSERT (1인 1표, 변경 가능)
- 마감(`ends_at`) 지난 투표는 400

#### 4-3. 투표 결과 조회 <완료>

- `GET /api/posts/{postId}/vote/result`
- 옵션별 `vote_count` + 본인 선택 옵션 ID 반환

### [Frontend]

#### 4-4. 투표 게시글 작성 페이지 (`/board/vote/write`) <완료>

- 선택지 추가/삭제 UI (최소 2개)
- MBTI 투표 모드 토글 (MBTI 16종 자동 선택지 생성)
- 마감일 DatePicker

#### 4-5. 투표 상세 페이지 (`/board/vote/[id]`) <완료>

- 투표 전: 선택지 라디오 버튼
- 투표 후: 결과 바 차트 (옵션별 득표율)
- 마감 표시 (D-day 또는 종료)

---

## Phase 5 — Debate 게시판

> Comment의 `debate_stance` 처리는 Phase 3에서 완료. 토론 메타 + UI만 추가.

### [Backend]

#### 5-1. 토론 게시글 작성 <완료>

- `POST /api/posts` — `type: DEBATE` + `proLabel`, `conLabel`, `endsAt` 포함
- `debate_posts` 함께 저장 (트랜잭션)

#### 5-2. 토론 현황 조회 <완료>

- `GET /api/posts/{postId}/debate/summary`
- 찬성/반대별 댓글 수, MBTI 분포 반환

### [Frontend]

#### 5-3. 토론 게시글 작성 페이지 (`/board/debate/write`) <완료>

- 찬성/반대 라벨 커스텀 입력
- 마감일 DatePicker

#### 5-4. 토론 상세 페이지 (`/board/debate/[id]`) <완료>

- 댓글 작성 전 스탠스 선택 모달 (찬성/반대)
- 좌우 분할 레이아웃: 찬성 댓글 | 반대 댓글
- 상단 현황 바: 찬성 N% vs 반대 N%

---

## Phase 5-5 — 게시글 작성 타입 전환 UX <완료>

> Phase 5 완료 후 진행. 세 게시판 작성 페이지가 모두 존재한 뒤에 구현.

### 목표

작성 중 게시글 타입을 전환해도 제목·내용이 유지되도록 한다.

---

## Phase 6 — MBTI 검사

> 핵심 차별점 기능. 검사 결과로 `mbti_verified = TESTED` 처리.

### [Backend]

#### 6-1. 검사 문항 조회 <완료>

- `GET /api/mbti/test/questions` — 정적 데이터

#### 6-2. 검사 결과 제출 <완료>

- `POST /api/mbti/test/submit` — 회원 전용
- **점수 계산은 프론트엔드가 담당**: 답변 × isReverse 반전 → 4개 지표 점수 누적
- 서버는 점수만 수신 후 MBTI를 독립 재도출 (클라이언트 조작 방어)
- `mbti_test_history` 저장 + `users.mbti_id` · `mbti_verified = TESTED` 업데이트
- **UX 흐름**: 검사 완료 → 프론트에서 결과 표시 → "MBTI를 기록하시겠어요?" 확인 모달
  - 네: `POST /submit` 호출 (미로그인 시 로그인 먼저, 점수는 sessionStorage 임시 보관)
  - 아니요: 아무것도 안 함

#### 6-3. 검사 이력 조회 <완료>

- `GET /api/users/me/mbti/history` — 회원 전용, 최신순

#### 6-4. MBTI 메타데이터 조회 <완료>

- `GET /api/mbti` — 전체 16종 + UNKNOWN
- `GET /api/mbti/{id}` — 단건

### [Frontend]

#### 6-5. MBTI 검사 페이지 (`/mbti/test`) (중요)개발 전 계획 먼저 <완료>

참고: mbti_quest_design.md

- 문항 스텝 UI (진행 바 포함)
- 결과 페이지: MBTI 유형 카드 + 공유 버튼
- 완료 후 마이페이지 뱃지 `TESTED`로 자동 갱신

#### 6-6. 전체 MBTI 유형 목록 페이지 (`/mbti`) <완료>

참고 디자인: 16personalities.com/ko/성격-유형

- `useQuery` — `GET /api/mbti` 전체 목록 조회, `id === "NONE"` 클라이언트 필터링
- 16개 유형을 4개 기질 그룹으로 분류 (클라이언트 정적 매핑)
  - 분석가 (NT): INTJ, INTP, ENTJ, ENTP
  - 외교관 (NF): INFJ, INFP, ENFJ, ENFP
  - 관리자 (SJ): ISTJ, ISFJ, ESTJ, ESFJ
  - 탐험가 (SP): ISTP, ISFP, ESTP, ESFP
- 그룹별 섹션 헤더 + 그 아래 카드 그리드 렌더링
  - 카드 구성: `iconUrl` 이미지, 유형 코드(`id`), 요약(`summary`), `colorCode` 테마 배경색
- 상단: "MBTI 검사 하러 가기" CTA 버튼 → `/mbti/test`

---

## Phase 7 — Admin

> 운영 기능. `UserRole.ADMIN` / `TESTER` 권한 체크 미들웨어 포함.

### [Backend]

#### 7-1. 유저 관리 <완료>

- `GET /api/admin/users` — 검색/필터 (이메일, 상태, 역할)
- `PATCH /api/admin/users/{id}/state` — 상태 변경
- `PATCH /api/admin/users/{id}/role` — 역할 변경

#### 7-2. 게시글/댓글 관리 <완료>

- `PATCH /api/admin/posts/{id}/visibility`
- `PATCH /api/admin/comments/{id}/visibility`

### [Frontend]

#### 7-3. 어드민 대시보드 (`/admin`) <완료>

- 유저 목록 테이블 + 상태/역할 변경 드롭다운
- 게시글/댓글 숨김 처리 버튼

---

## Phase 8 — 회원 정보 찾기

> 비로그인 상태에서 이메일 또는 비밀번호를 분실한 사용자 지원. 소셜 계정 분기 처리 포함.

### 설계 원칙

- 이메일 찾기: 닉네임으로 조회 → 마스킹된 이메일 반환 (소셜 계정 여부 구분)
- 비밀번호 찾기: 이메일로 리셋 링크 발송 → 토큰 검증 → 새 비밀번호 설정
- 보안: 이메일 열거 공격 방지 — 이메일 존재 여부 무관하게 동일 응답 반환

### 계정 상태별 처리 정책

| 상태            | 이메일 찾기 | 비밀번호 재설정                       |
| --------------- | ----------- | ------------------------------------- |
| `NORMAL`        | 허용        | 허용                                  |
| `PENDING`       | 허용        | 거부 (이메일 인증 미완료 계정)        |
| `LOCKED`        | 허용        | 허용 (재설정으로 잠금 해제 수단 제공) |
| `SHADOW_BANNED` | 허용        | 허용                                  |
| `WITHDRAWN`     | 404         | 404                                   |
| `PERMANENT_BAN` | 허용        | 거부                                  |

### [Backend]

#### 8-0. Security Config 수정 <완료>

- 다음 3개 경로 퍼블릭 허용 추가:
  - `/api/auth/find-email`
  - `/api/auth/password-reset/request`
  - `/api/auth/password-reset/confirm`

#### 8-1. 이메일 찾기 <완료>

- `POST /api/auth/find-email`
- 요청: `{ "nickname": "홍길동" }`
- 동작:
  - `UserRepository.findByNickname()` 로 유저 조회, 없으면 404
  - `WITHDRAWN` 상태면 404
  - 이메일 마스킹 처리 후 반환 — `@` 앞 2자 노출, 나머지 `***` (예: `ab***@gmail.com`)
  - 소셜 전용 계정(`socialProvider != null`)이면 마스킹 이메일과 함께 `isSocialOnly: true` 반환 — "소셜 로그인으로 가입된 계정입니다" 안내용
- 응답: `{ "maskedEmail": "ab***@gmail.com", "isSocialOnly": false }`
- 신규 파일: `FindEmailRequest.java`, `FindEmailResponse.java`
- `UserRepository`: `findByNickname()` 메서드 추가

#### 8-2. 비밀번호 재설정 요청 <완료>

- `POST /api/auth/password-reset/request`
- 요청: `{ "email": "user@example.com" }`
- 동작:
  - 항상 동일 응답 반환 (이메일 열거 방지)
  - 유저가 존재하고 로컬 계정이며 `NORMAL` / `LOCKED` 상태인 경우에만 실제 발송:
    1. `PW:RESET:EMAIL:{email}` 조회 → 기존 `oldToken` 존재 시 `PW:RESET:{oldToken}` 삭제 (이전 링크 무효화)
    2. 새 UUID 토큰 생성
    3. `PW:RESET:{newToken}` → `email` 저장, TTL: 30분
    4. `PW:RESET:EMAIL:{email}` → `newToken` 저장, TTL: 30분 (덮어쓰기)
    5. 이메일 발송: 프론트 `/account/reset-password?token={newToken}` 링크
  - `PENDING` / `PERMANENT_BAN` / 소셜 계정 / 미존재 이메일이면 아무것도 하지 않음
- 응답: `{ "message": "이메일이 발송되었습니다." }` (항상 200)
- `TokenRedisService` 추가 메서드:
  - `savePasswordResetToken(token, email)` — `PW:RESET:{token}` + `PW:RESET:EMAIL:{email}` 쌍 저장
  - `getEmailByPasswordResetToken(token)` — `PW:RESET:{token}` 조회
  - `getPasswordResetTokenByEmail(email)` — `PW:RESET:EMAIL:{email}` 조회 (재요청 시 기존 토큰 삭제용)
  - `deletePasswordResetToken(token, email)` — 두 키 모두 삭제
- `EmailService`: `sendPasswordResetEmail()` 추가
- 신규 파일: `PasswordResetRequestBody.java`

#### Redis Key 설계 (신규)

| Key                      | Value | TTL  |
| ------------------------ | ----- | ---- |
| `PW:RESET:{token}`       | email | 30분 |
| `PW:RESET:EMAIL:{email}` | token | 30분 |

#### 8-3. 비밀번호 재설정 확인 <완료>

- `POST /api/auth/password-reset/confirm`
- 요청: `{ "token": "uuid", "newPassword": "새비밀번호" }`
- 동작:
  - `PW:RESET:{token}` 조회 → 없으면 400 (만료/무효)
  - 이메일로 유저 조회
  - 새 비밀번호 BCrypt 해싱 → `user.updatePassword()` 업데이트
  - `PW:RESET:{token}` + `PW:RESET:EMAIL:{email}` 두 키 모두 삭제 (재사용 방지)
  - 해당 유저의 **모든 RT 삭제** → 전 기기 강제 로그아웃
- 신규 파일: `PasswordResetConfirmRequest.java` (`@NotBlank` + `@Size(min=8)` 검증)
- `User.java`: `updatePassword(String encodedPw)` 메서드 추가
- `ErrorCode`: `PASSWORD_RESET_TOKEN_INVALID` 추가

### [Frontend]

#### 8-4. 로그인 페이지 진입점 추가 (`/login`) <완료>

- 로그인 폼 하단에 "이메일 찾기 | 비밀번호 찾기" 링크 추가 → `/account/find`

#### 8-5. 회원 정보 찾기 페이지 (`/account/find`) <완료>

- 탭 UI: **이메일 찾기** | **비밀번호 찾기**
- **이메일 찾기 탭**:
  - 닉네임 입력 → "찾기" 버튼
  - 성공: 마스킹 이메일 표시
    - `isSocialOnly: true`이면 이메일 아래 "소셜 로그인(Google/Kakao)으로 가입된 계정입니다" 안내 추가
  - 실패(404): "해당 닉네임을 찾을 수 없습니다" 메시지
- **비밀번호 찾기 탭** (2단계 Step UI):
  - Step 1: 이메일 입력 → "재설정 링크 발송" 버튼
  - Step 2: "이메일이 발송되었습니다. 메일함을 확인해 주세요." 안내 화면 + "다시 받기" 버튼(Step 1으로 복귀)

#### 8-6. 비밀번호 재설정 페이지 (`/account/reset-password`) v

- 마운트 시 URL 파라미터 `?token` 없으면 즉시 `/account/find`로 리다이렉트
- 새 비밀번호 / 비밀번호 확인 입력 폼
- 유효성: 8자 이상, 두 값 일치 여부 클라이언트 검증
- 성공 → `/login` 리다이렉트 + "비밀번호가 변경되었습니다" 토스트
- API 400 응답(토큰 만료/무효) 시 에러 안내 + `/account/find`로 이동하는 "다시 요청하기" 링크

#### API 및 타입

- `lib/api/auth.ts`: `findEmail`, `requestPasswordReset`, `confirmPasswordReset` 함수 추가
- `types/auth.ts`: `FindEmailResponse`, `PasswordResetRequest` 타입 추가

---

## 공통 작업 (각 Phase 완료 시 병행)

**Backend**

- [ ] Flyway 마이그레이션 파일 (`V{n}__*.sql`) — DDL, 트리거, 인덱스 포함
- [ ] MapStruct Mapper (Entity ↔ DTO)
- [ ] OpenAPI `@Operation` / `@ApiResponse` 어노테이션
- [ ] 통합 테스트 (Phase별 핵심 시나리오)

**Frontend**

- [ ] API 함수 모듈 (`lib/api/`) — Phase별 엔드포인트 함수 분리
- [ ] 커스텀 훅 (`hooks/`) — `useAuth`, `usePosts`, `useComments` 등
- [ ] 에러 처리 — `ApiErrorBoundary` + Toast 알림

---

## 개발 순서 요약

```
Phase 0 (프론트 기반)
 → Phase 1 Backend -> Test Code → Phase 1 Frontend
 → Phase 2 Backend -> Test Code → Phase 2 Frontend
 → Phase 3 Backend -> Test Code → Phase 3 Frontend
 → Phase 4 Backend -> Test Code → Phase 4 Frontend
 → Phase 5 Backend -> Test Code → Phase 5 Frontend
 → Phase 6 Backend -> Test Code → Phase 6 Frontend
 → Phase 7 Backend -> Test Code → Phase 7 Frontend
 → Phase 8 Backend -> Test Code → Phase 8 Frontend
```
