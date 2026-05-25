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

#### 4-4. 투표 게시글 작성 페이지 (`/board/vote/write`)

- 선택지 추가/삭제 UI (최소 2개)
- MBTI 투표 모드 토글 (MBTI 16종 자동 선택지 생성)
- 마감일 DatePicker

#### 4-5. 투표 상세 페이지 (`/board/vote/[id]`)

- 투표 전: 선택지 라디오 버튼
- 투표 후: 결과 바 차트 (옵션별 득표율)
- 마감 표시 (D-day 또는 종료)

---

## Phase 5 — Debate 게시판

> Comment의 `debate_stance` 처리는 Phase 3에서 완료. 토론 메타 + UI만 추가.

### [Backend]

#### 5-1. 토론 게시글 작성

- `POST /api/posts` — `type: DEBATE` + `proLabel`, `conLabel`, `endsAt` 포함
- `debate_posts` 함께 저장 (트랜잭션)

#### 5-2. 토론 현황 조회

- `GET /api/posts/{postId}/debate/summary`
- 찬성/반대별 댓글 수, MBTI 분포 반환

### [Frontend]

#### 5-3. 토론 게시글 작성 페이지 (`/board/debate/write`)

- 찬성/반대 라벨 커스텀 입력
- 마감일 DatePicker

#### 5-4. 토론 상세 페이지 (`/board/debate/[id]`)

- 댓글 작성 전 스탠스 선택 모달 (찬성/반대)
- 좌우 분할 레이아웃: 찬성 댓글 | 반대 댓글
- 상단 현황 바: 찬성 N% vs 반대 N%

---

## Phase 6 — MBTI 검사

> 핵심 차별점 기능. 검사 결과로 `mbti_verified = TESTED` 처리.

### [Backend]

#### 6-1. 검사 문항 조회

- `GET /api/mbti/test/questions` — 정적 데이터

#### 6-2. 검사 결과 제출

- `POST /api/mbti/test/submit`
- 점수 계산 → `mbti_test_history` 저장
- `users.mbti_id` + `mbti_verified = TESTED` 업데이트

#### 6-3. 검사 이력 조회

- `GET /api/users/me/mbti/history`

#### 6-4. MBTI 메타데이터 조회

- `GET /api/mbti` — 전체 16종 + UNKNOWN
- `GET /api/mbti/{id}` — 단건

### [Frontend]

#### 6-5. MBTI 검사 페이지 (`/mbti/test`)

- 문항 스텝 UI (진행 바 포함)
- 결과 페이지: MBTI 유형 카드 + 공유 버튼
- 완료 후 마이페이지 뱃지 `TESTED`로 자동 갱신

---

## Phase 7 — Admin

> 운영 기능. `UserRole.ADMIN` / `TESTER` 권한 체크 미들웨어 포함.

### [Backend]

#### 7-1. 유저 관리

- `GET /api/admin/users` — 검색/필터 (이메일, 상태, 역할)
- `PATCH /api/admin/users/{id}/state` — 상태 변경
- `PATCH /api/admin/users/{id}/role` — 역할 변경

#### 7-2. 게시글/댓글 관리

- `PATCH /api/admin/posts/{id}/visibility`
- `PATCH /api/admin/comments/{id}/visibility`

### [Frontend]

#### 7-3. 어드민 대시보드 (`/admin`)

- 유저 목록 테이블 + 상태/역할 변경 드롭다운
- 게시글/댓글 숨김 처리 버튼

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
 → Phase 1 Backend → Phase 1 Frontend
 → Phase 2 Backend → Phase 2 Frontend
 → Phase 3 Backend → Phase 3 Frontend
 → Phase 4 Backend → Phase 4 Frontend
 → Phase 5 Backend → Phase 5 Frontend
 → Phase 6 Backend → Phase 6 Frontend
 → Phase 7 Backend → Phase 7 Frontend
```
