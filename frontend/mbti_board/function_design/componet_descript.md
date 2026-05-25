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

---

# Phase 2 — 생성 파일 요약 (FREE 게시판)

## Lib

- `lib/api/mbti.ts` — fetchMbtiList API 함수; MBTI 전체 목록 조회 (staleTime Infinity 활용)
- `lib/api/post.ts` — fetchPosts, fetchPostById, createPost, updatePost, deletePost API 함수; 비회원 삭제는 X-Guest-Password 헤더 전달
- `lib/safeRedirect.ts` — toSafeRedirect 유틸; 외부 URL·프로토콜 상대 URL Open Redirect 차단, 내부 경로("/")만 허용

## Hooks

- `hooks/usePosts.ts` — usePostList(무한 스크롤 InfiniteQuery), usePostDetail(목록 캐시 initialData 활용), postKeys 쿼리키 팩토리

## Components

- `components/post/PostCard.tsx` — 게시글 목록 카드; 제목·작성자 MBTI 배지·댓글/조회수·상대 시간 표시, 클릭 시 상세 페이지 이동
- `components/post/PostListFilter.tsx` — 정렬 탭(LATEST/COMMENTS/VIEWS) + 작성자·대상 MBTI 드롭다운 필터
- `components/post/RichContentEditor.tsx` — Tiptap 기반 리치텍스트 에디터 (SSR 비활성 dynamic import); Bold/Italic/H2/H3/BulletList/Code 툴바
- `components/post/RichContentViewer.tsx` — Tiptap read-only 렌더러 (SSR 비활성 dynamic import); content undefined 시 스켈레톤 표시
- `components/post/GuestPasswordModal.tsx` — 비회원 수정·삭제 시 비밀번호 확인 모달; MODAL_ACTION export, isPending 비활성화, error 메시지 표시

## Pages

- `app/(main)/board/free/page.tsx` — 자유게시판 목록; useInfiniteQuery + IntersectionObserver 무한 스크롤, 정렬·MBTI 필터
- `app/(main)/board/free/write/page.tsx` — 게시글 작성 폼; 회원/비회원 분기(guestPassword 조건부), Tiptap 빈값 검증
- `app/(main)/board/free/[id]/page.tsx` — 상세 서버 래퍼; await params + ID 유효성 검증 후 PostDetailContent 렌더링
- `app/(main)/board/free/[id]/PostDetailContent.tsx` — 상세 클라이언트 컴포넌트; 인라인 수정·삭제, GuestPasswordModal, initialData 캐시 활용

---

# Phase 2 (보완) — shadcn/ui 도입

## Lib

- `lib/utils.ts` — tailwind-merge + clsx 합성 cn() 유틸; shadcn/ui 컴포넌트 className 병합에 사용

## Components (UI Primitives)

- `components/ui/button.tsx` — shadcn/ui Button; variant(default/outline/ghost 등)·size prop, @radix-ui/react-slot(asChild 패턴) 기반
- `components/ui/input.tsx` — shadcn/ui Input; Tailwind 스타일 포함, 네이티브 input 래퍼
- `components/ui/dialog.tsx` — shadcn/ui Dialog; Portal·Overlay·Content·Title·Description·Footer 포함, @radix-ui/react-dialog 기반

---

# Phase 3 — 댓글 도메인

## Hooks

- `hooks/useComments.ts` — commentKeys 쿼리키 팩토리 + useComments(postId); GET /api/posts/{postId}/comments 조회
- `hooks/useReactionDebounce.ts` — 리액션 디바운스 훅; 클릭 즉시 localState 반영, 500ms 후 요청 전송, 최종 상태=초기 상태면 요청 생략, 언마운트 시 잔여 디바운스 즉시 실행

## Lib

- `lib/api/comment.ts` — fetchComments API 함수; apiClient로 댓글 목록 조회
- `lib/dateUtils.ts` — 날짜 포맷 유틸; formatDate(iso) → ko-KR 로케일 날짜 문자열

## Components

- `components/post/CommentSection.tsx` — 게시글 상세 하단 댓글 섹션; 계층형(루트→대댓글 들여쓰기) 렌더링, 로딩 스켈레톤, 빈 상태 표시
- `components/post/CommentForm.tsx` — 댓글 작성 폼; 로그인 유저는 바로 제출, 비회원은 GuestPasswordModal(CREATE)로 비밀번호 입력 후 제출
- `components/post/CommentReactionButtons.tsx` — 좋아요/싫어요 토글 버튼; 낙관적 업데이트(onMutate 캐시 패치 → onError 롤백 → onSettled 재검증), 비로그인 시 비활성화
