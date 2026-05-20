# Phase 0 — 생성 파일 요약

## Types

- `types/api.ts` — 백엔드 공통 응답 포맷 `ApiResponse<T>`, `PageResponse<T>` 타입 정의
- `types/auth.ts` — 로그인·회원가입 요청 및 응답 타입 정의
- `types/user.ts` — `User`, `MbtiVerified`, `UserRole`, `UserState` 등 유저 도메인 타입
- `types/post.ts` — `Post`, `VoteOption`, `DebateSummary` 등 게시글 도메인 타입
- `types/comment.ts` — `Comment`, `DebateStance`, `ReactionType` 등 댓글 도메인 타입

## Store

- `store/authStore.ts` — Zustand로 Access Token을 메모리에 저장·조회하는 전역 인증 상태

## Lib

- `lib/authChannel.ts` — BroadcastChannel을 이용한 탭 간 AT 갱신 및 로그아웃 이벤트 동기화 유틸
- `lib/apiClient.ts` — axios 인스턴스; 요청 시 AT 헤더 주입, 401 수신 시 silent refresh 후 원 요청 재시도

## Components

- `components/common/QueryProvider.tsx` — TanStack Query `QueryClient`를 앱 전역에 제공하는 클라이언트 컴포넌트
- `components/common/AuthSyncProvider.tsx` — 앱 마운트 시 silent refresh로 AT 복구, BroadcastChannel 탭 동기화 구독
- `components/common/MbtiBadge.tsx` — MBTI 유형과 인증 상태(`TESTED`/`SELF`/`DEFAULT`)를 색상 배지로 표시하는 컴포넌트
- `components/common/ApiErrorBoundary.tsx` — React 렌더 에러를 포착해 에러 메시지와 재시도 버튼을 표시하는 클래스형 Error Boundary
- `components/common/Header.tsx` — AT 존재 여부로 로그인/로그아웃 버튼을 전환하는 상단 고정 헤더
- `components/common/Sidebar.tsx` — 자유·투표·토론 게시판 탭 링크와 현재 경로 활성화 표시 사이드바

## Root

- `middleware.ts` — RT 쿠키 부재 시 `/my`, `/board/*/write` 경로를 `/login`으로 리다이렉트하는 라우트 보호 미들웨어

---

# Phase 1 — 생성 파일 요약

## Types

- `types/user.ts` — `MyProfileResponse` 추가: 백엔드 `GET /api/users/me` 응답 타입 (mbtiColorCode, isSocialOnly 포함)

## Lib

- `lib/apiEndpoints.ts` — `users` 섹션 추가: me, mePassword, meWithdrawalVerify, byId 엔드포인트 상수
- `lib/api/user.ts` — `fetchMyProfile`, `updateMyProfile` API 함수

## Components

- `components/my/MyPageContent.tsx` — 내 프로필 조회(useQuery)·수정(useMutation) 클라이언트 컴포넌트; MBTI 변경 시 배지 즉시 갱신

## Pages

- `app/my/page.tsx` — `/my` 서버 컴포넌트; MyPageContent 렌더링

---

# Phase 1-5 — 생성 파일 요약 (로그인 / 회원가입)

## Lib

- `lib/api/auth.ts` — login, signup, sendEmailVerification, confirmEmailVerification API 함수
- `lib/apiEndpoints.ts` — auth 섹션 추가: login, signup, reissue, emailVerifySend, emailVerifyConfirm (기존 refresh→reissue 오류 수정)

## Components

- `components/auth/LoginForm.tsx` — 이메일·비밀번호 로그인 폼 + 소셜 로그인 버튼(Google/Kakao); redirectTo prop으로 로그인 후 이동 경로 수신
- `components/auth/SignupContent.tsx` — 3단계 회원가입 흐름 (이메일 인증 발송 → 대기 → 정보 입력 → 완료); sessionStorage로 이메일 임시 보관
- `components/auth/SignupVerifyContent.tsx` — 이메일 인증 링크 도착 페이지; 토큰 자동 확인 후 SignupContent form 단계로 리다이렉트

## Pages

- `app/login/page.tsx` — `/login` 서버 컴포넌트; searchParams의 redirect 파라미터를 LoginForm에 전달
- `app/signup/page.tsx` — `/signup` 서버 컴포넌트; step·email 파라미터로 SignupContent 초기 단계 제어
- `app/signup/verify/page.tsx` — `/signup/verify` 서버 컴포넌트; token 파라미터를 SignupVerifyContent에 전달
