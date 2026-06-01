# Phase 0 — 생성 파일 요약

## Types

- `types/api.ts` — 백엔드 공통 응답 포맷 `ApiResponse<T>`, `PageResponse<T>` 타입 정의
- `types/auth.ts` — 로그인·회원가입 요청 및 응답 타입 정의
- `types/user.ts` — `User`, `MbtiVerified`, `UserRole`, `UserState` 등 유저 도메인 타입
- `types/post.ts` — `Post`, `VoteOption`, `DebateSummary` 등 게시글 도메인 타입
- `types/comment.ts` — `Comment`, `DebateStance`, `ReactionType` 등 댓글 도메인 타입

## Store

- `store/authStore.ts` — Zustand로 Access Token을 메모리에 저장·조회하는 전역 인증 상태

## Theme

- `components/theme/ThemeSwitcher.tsx` — 테마 선택 버튼 (라이트/다크/네이비/로즈); next-themes `useTheme` 사용

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
- `app/(main)/board/free/write/page.tsx` — 자유게시판 작성; PostWriteForm 사용, 비회원 비밀번호 필드 조건부 children 주입, Tiptap 빈값 검증
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

---

# Phase 4 — 투표 게시판

## Lib

- `lib/api/vote.ts` — castVote(postId, optionId), fetchVoteResult(postId) API 함수; 투표 참여 및 결과 조회
- `lib/editor.ts` — Tiptap 관련 공통 유틸; EMPTY_TIPTAP 빈 문서 상수, isTiptapEmpty 빈값 검증 함수

## Components

- `components/post/PostWriteForm.tsx` — 게시글 작성 공용 폼 셸; 제목·대상MBTI·내용 에디터·취소/등록 버튼 공통 렌더링, 게시판별 추가 필드는 children으로 주입
- `components/post/PostCard.tsx` — href prop으로 상세 경로를 외부에서 주입; 각 목록 페이지가 ROUTES 상수로 계산해 전달. `showType` prop 시 게시판 유형 뱃지(자유/투표/토론) 표시

## Pages

- `app/(main)/board/vote/page.tsx` — 투표게시판 목록; useInfiniteQuery + IntersectionObserver 무한 스크롤, 정렬·MBTI 필터
- `app/(main)/board/vote/write/page.tsx` — 투표 게시글 작성; PostWriteForm 사용, 일반/MBTI 모드 토글·선택지 추가·삭제·마감일 children 주입
- `app/(main)/board/vote/[id]/page.tsx` — 투표 상세 서버 래퍼; await params + ID 유효성 검증 후 VoteDetailContent 렌더링
- `app/(main)/board/vote/[id]/VoteDetailContent.tsx` — 투표 상세 클라이언트 컴포넌트; 진행 중(라디오 투표)·종료(결과 바) 분기, 투표 변경 지원, 마감 D-day 표시

---

# Phase 5 — 토론 게시판

## Lib

- `lib/api/post.ts` — createVotePost(POST /api/posts/vote), createDebatePost(POST /api/posts/debate) API 함수 추가

## Pages

- `app/(main)/board/debate/page.tsx` — 토론게시판 목록; useInfiniteQuery + IntersectionObserver 무한 스크롤, 정렬·MBTI 필터
- `app/(main)/board/debate/write/page.tsx` — /board/write?type=DEBATE 로 redirect
- `app/(main)/board/debate/[id]/page.tsx` — 토론 상세 서버 래퍼; await params + ID 유효성 검증 후 DebateDetailContent 렌더링
- `app/(main)/board/debate/[id]/DebateDetailContent.tsx` — 토론 상세 클라이언트 컴포넌트; 찬반 비율 바·인원 수 표시, 인라인 수정·삭제, GuestPasswordModal, 찬반 선택 버튼(비회원 disable)

---

# Phase 6 — 통합 작성 페이지

## Components

- `components/post/PostWriteForm.tsx` — beforeTitle slot 추가; 폼 상단(제목 위)에 타입 토글 등 커스텀 콘텐츠 삽입 가능

## Pages

- `app/(main)/board/write/page.tsx` — 통합 작성 서버 컴포넌트; searchParams.type(FREE/VOTE/DEBATE)을 파싱해 WriteFormContent에 initialType 전달
- `app/(main)/board/write/WriteFormContent.tsx` — 통합 작성 클라이언트 컴포넌트; 자유글·투표·토론 타입 토글 UI, 타입별 조건부 필드(투표 선택지, 찬반 레이블, 마감일, 비회원 비밀번호), 타입에 따라 올바른 API 호출 후 해당 게시판으로 리다이렉트
- `app/(main)/board/free/write/page.tsx` — /board/write 로 redirect
- `app/(main)/board/vote/write/page.tsx` — /board/write?type=VOTE 로 redirect

---

# Phase 6 (dev_steps) — MBTI 검사

## Pages

- `app/(main)/mbti/test/page.tsx` — `/mbti/test` 서버 래퍼; MbtiTestContent 렌더링
- `app/(main)/mbti/test/MbtiTestContent.tsx` — MBTI 검사 클라이언트 컴포넌트; 문항 스텝 UI·진행 바·5지선다 채점·결과 카드·저장 모달, 비로그인 시 sessionStorage 점수 임시 저장 후 로그인 복귀 자동 제출, 재검사 시 캐시 무효화로 새 문항 랜덤 추출

---

# Phase 6-6 — MBTI 유형 목록 페이지

## Types

- `types/mbti.ts` — `MBTI_GROUPS` 상수 추가: 4개 기질 그룹(분석가/외교관/관리자/탐험가)별 MBTI ID 배열

## Components

- `components/mbti/MbtiTypeCard.tsx` — 단일 MBTI 유형 카드; iconUrl 이미지(없으면 colorCode 원형 폴백), 유형 코드(id), 요약(summary) 표시, 상단 colorCode 테마 보더

## Pages

- `app/(main)/mbti/page.tsx` — `/mbti` 서버 래퍼; MbtiListContent 렌더링
- `app/(main)/mbti/MbtiListContent.tsx` — MBTI 유형 목록 클라이언트 컴포넌트; useMbtiList로 전체 조회 후 MBTI_GROUPS 기준 4개 섹션 그리드, 로딩 스켈레톤, NONE 필터링, 검사 CTA 버튼

---

# Phase 7-3 — 어드민 대시보드

## Types

- `types/admin.ts` — 어드민 전용 응답·요청 타입; AdminUserSummaryResponse, AdminPostSummaryResponse, AdminCommentSummaryResponse, 변경 요청 DTO, 필터 파라미터, 상태·역할 레이블 상수

## Lib

- `lib/apiEndpoints.ts` — admin 섹션 추가: users, userState, userRole, posts, postVisibility, comments, commentVisibility 엔드포인트 상수
- `lib/routes.ts` — ROUTES.admin 추가: "/admin"
- `lib/api/admin.ts` — fetchAdminUsers, changeUserState, changeUserRole, fetchAdminPosts, changePostVisibility, fetchAdminComments, changeCommentVisibility API 함수

## Hooks

- `hooks/useAdmin.ts` — adminKeys 쿼리키 팩토리 + useAdminUsers/Posts/Comments(useQuery), useChangeUserState/Role/PostVisibility/CommentVisibility(useMutation) — onSuccess 시 해당 도메인 쿼리 무효화

## Components

- `components/admin/AdminDashboard.tsx` — 어드민 대시보드 탭 컨테이너; 유저·게시글·댓글 관리 탭 전환, 각 탭에 해당 테이블 컴포넌트 렌더링
- `components/admin/AdminUserTable.tsx` — 유저 관리 테이블; 이메일·상태·역할 필터, 역할/상태 인라인 드롭다운 변경(useMutation), 페이지네이션
- `components/admin/AdminPostTable.tsx` — 게시글 관리 테이블; 유형·가시성 필터, 숨김/복구 버튼(useMutation), 페이지네이션
- `components/admin/AdminCommentTable.tsx` — 댓글 관리 테이블; 게시글ID·가시성 필터, 숨김/복구 버튼(useMutation), 페이지네이션

## Middleware

- `middleware.ts` — /admin/:path* 경로 추가: session 쿠키 없으면 /login 리다이렉트 (ADMIN 권한 검증은 API 서버에서 수행)

## Pages

- `app/(main)/admin/page.tsx` — `/admin` 서버 컴포넌트; AdminDashboard 렌더링

---

# Phase 8 (8-4 / 8-5) — 회원 정보 찾기

## Types

- `types/auth.ts` — `FindEmailRequest`, `FindEmailResponse`, `PasswordResetRequest` 타입 추가

## Lib

- `lib/apiEndpoints.ts` — auth 섹션에 findEmail, passwordResetRequest 엔드포인트 상수 추가
- `lib/routes.ts` — `ROUTES.accountFind` 추가: "/account/find"
- `lib/api/auth.ts` — `findEmail`, `requestPasswordReset` API 함수 추가

## Components

- `components/auth/FindAccountContent.tsx` — 이메일 찾기(닉네임 입력·마스킹 결과·소셜 안내) / 비밀번호 찾기(2단계: 이메일 입력 → 발송 완료) 탭 UI 클라이언트 컴포넌트
- `components/auth/LoginForm.tsx` — 로그인 폼 하단에 "이메일 찾기 | 비밀번호 찾기" 링크 추가 (8-4)

## Pages

- `app/(auth)/account/find/page.tsx` — `/account/find` 서버 컴포넌트; searchParams.tab("email"/"password")으로 FindAccountContent 초기 탭 제어

---

# Phase 8-6 — 비밀번호 재설정 페이지

## Types

- `types/auth.ts` — `PasswordResetConfirmRequest` 타입 추가 (`token`, `newPassword`)

## Lib

- `lib/apiEndpoints.ts` — auth 섹션에 `passwordResetConfirm` 엔드포인트 상수 추가
- `lib/routes.ts` — `ROUTES.accountResetPassword` 추가: "/account/reset-password"
- `lib/api/auth.ts` — `confirmPasswordReset` API 함수 추가

## Components

- `components/auth/ResetPasswordContent.tsx` — 비밀번호 재설정 클라이언트 컴포넌트; 마운트 시 token 없으면 /account/find 리다이렉트, 새 비밀번호·확인 입력 폼, 클라이언트 유효성 검증(8자 이상·일치 여부), 성공 시 /login 리다이렉트+토스트, API 400 시 에러 안내+"다시 요청하기" 링크

## Pages

- `app/(auth)/account/reset-password/page.tsx` — `/account/reset-password` 서버 컴포넌트; searchParams.token을 ResetPasswordContent에 전달

---

# Phase 9 — 마이페이지 기능 확장

## Types

- `types/post.ts` — `PostSummaryResponse`에 `typeCode: PostType` 필드 추가; `MyPostListParams` 인터페이스 추가 (type·sort·page·size 파라미터)

## Lib

- `lib/apiEndpoints.ts` — users 섹션에 `mePosts: "/api/users/me/posts"` 상수 추가
- `lib/api/post.ts` — `fetchMyPosts(params: MyPostListParams)` API 함수 추가; `GET /api/users/me/posts` 호출

## Components

- `components/my/MyPostsSection.tsx` — 내가 작성한 게시글 목록 클라이언트 컴포넌트; 전체/자유글/투표/토론 탭 필터, useQuery로 페이지 단위 조회, typeCode별 상세 링크 생성(ROUTES 활용), 페이지네이션 이전/다음 버튼, 빈 상태 안내
- `components/my/MyPageContent.tsx` — MyPostsSection 추가 (프로필 카드 하단); 회원 탈퇴 "위험 구역" 섹션 + 2단계 모달(탈퇴 안내 → OTP 입력) 추가

## 9-2 Lib 추가

- `lib/apiEndpoints.ts` — users 섹션에 `meWithdrawal: "/api/users/me"` 상수 추가 (DELETE 탈퇴 엔드포인트)
- `lib/api/user.ts` — `sendWithdrawalOtp()`, `withdraw(code)` API 함수 추가; OTP 발송(`POST /api/users/me/withdrawal-verify`) 및 탈퇴 실행(`DELETE /api/users/me`)

---

# Main Page — 전체 게시판 홈

## Pages

- `app/(main)/page.tsx` — `/` 홈 클라이언트 컴포넌트; type 없이 GET /api/posts 조회해 FREE·VOTE·DEBATE 전체 혼합 피드, showType PostCard 뱃지로 유형 구분, IntersectionObserver 무한 스크롤, 정렬·MBTI 필터

## Components

- `components/post/PostCard.tsx` — `showType` prop 추가: 게시판 유형 뱃지(자유=파랑/투표=보라/토론=주황) 조건부 표시

---

## Components (공통)

- `components/common/Logo.tsx` — MBTI 4개 차원을 4색 정사각형 아이콘으로 표현한 로고 컴포넌트; `size`(sm/md/lg)·`asLink`(홈 링크 여부) prop 지원
- `components/auth/SocialLoginButton.tsx` — 소셜 로그인 버튼 공용 컴포넌트; `provider`(google/kakao)·`redirectTo` prop으로 OAuth2 리다이렉트 처리, 스타일·경로는 PROVIDER_CONFIG 상수로 관리
- `components/common/Toast.tsx` — 완전 커스텀 토스트 시스템; `ToastProvider`(루트 마운트), `toast.success/error/info/warning()` 모듈 레벨 싱글턴으로 훅 없이 어디서든 호출 가능, 슬라이드인 애니메이션·자동 닫힘·수동 닫기 지원
- `components/common/FloatingLabelInput.tsx` — 플로팅 라벨 input 베이스 컴포넌트; `rightSlot` prop으로 우측 버튼 주입, `hint` prop으로 포커스 시 힌트 텍스트 표시 지원
- `components/common/EmailInput.tsx` — FloatingLabelInput 기반 이메일 전용 래퍼; 메일 아이콘·type email·autoComplete email 고정, 자동완성 기능 확장 예정
- `components/common/PasswordInput.tsx` — FloatingLabelInput 기반 비밀번호 전용 래퍼; show/hide 토글 상태 내장
- `components/common/MbtiSelect.tsx` — `@radix-ui/react-select` 기반 MBTI 선택 드롭다운; MBTI_GROUPS 그룹화·UNKNOWN→"미설정" 변환·체크 아이콘 표시 지원; `value·onChange·mbtiList·disabled` prop
- `components/common/LabeledFilterBox.tsx` — 좌측 라벨 + 우측 children을 테두리로 감싸는 필터 래퍼 컴포넌트; `label·children` prop
- `components/post/BoardTypeNav.tsx` — 전체·자유글·투표글·토론글 탭 링크; usePathname으로 현재 경로에 따라 활성 탭 강조
