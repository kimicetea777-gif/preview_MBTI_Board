# Phase 0 — 생성 파일 요약

## Route Handlers

- `app/api/upload/route.ts` — 이미지 업로드 프록시: Sharp WebP 변환 후 백엔드에 X-Internal-Key로 전달
- `app/api/images/route.ts` — 미저장 이미지 취소 프록시: X-Internal-Key 주입 후 백엔드 DELETE 전달 (브라우저에서 키 노출 방지)

## My Page

- `components/my/MyPostsPreview.tsx` — 마이페이지 내 게시글 프리뷰 (최근 5개, QueryResetErrorBoundary + Suspense + useSuspenseQuery 패턴)
- `components/admin/AdminAlertContent.tsx` — 어드민 이메일 알림 설정 페이지 — AlertType별 활성화/음소거 상태 조회 및 토글

## Types

- `components/common/ClearableInput.tsx` — value가 있을 때 우측에 X(지우기) 버튼이 표시되는 입력창 래퍼

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
- `components/common/MbtiAvatar.tsx` — MBTI 유형의 colorCode 방사형 그라데이언트 + 마스크 처리된 이미지를 원형으로 표시; `size` prop으로 sm(96px)/lg(160px) 선택, imageUrl 없으면 그라데이언트 원형만 표시
- `components/common/MbtiBadge.tsx` — MBTI 유형과 인증 상태(`TESTED`/`SELF`/`DEFAULT`)를 색상 배지로 표시; `TESTED`/`SELF` 클릭 시 `MbtiTypeCard` 팝오버(portal, fixed 위치) 표시, 바깥 클릭으로 닫기
- `components/common/ApiErrorBoundary.tsx` — React 렌더 에러를 포착해 에러 메시지와 재시도 버튼을 표시하는 클래스형 Error Boundary
- `components/common/Header.tsx` — AT 존재 여부로 로그인 버튼/마이페이지 아이콘을 전환하는 상단 고정 헤더 (로그아웃은 마이페이지로 이동)
- `components/common/LogoutButton.tsx` — 로그아웃 처리(로그아웃 API 호출·AT 초기화·로그인 페이지 이동)를 캡슐화한 버튼; `className` prop으로 스타일 주입
- `components/auth/SocialLoginButtons.tsx` — Google·Kakao 소셜 로그인 버튼 목록; `lastUsedMethod` 전달 시 마지막 로그인 제공자 말풍선 표시
- `components/common/Sidebar.tsx` — 자유·투표·토론 게시판 탭 링크와 현재 경로 활성화 표시 사이드바

## Root

- `middleware.ts` — RT 쿠키 부재 시 `/my`, `/board/*/write` 경로를 `/login`으로 리다이렉트하는 라우트 보호 미들웨어

---

# Phase 1 — 생성 파일 요약

## Types

- `types/user.ts` — `MyProfileResponse` 추가: 백엔드 `GET /api/users/me` 응답 타입 (isSocialOnly, hasPhone 포함)

## Lib

- `lib/apiEndpoints.ts` — `users` 섹션 추가: me, mePassword, meWithdrawalVerify, byId 엔드포인트 상수
- `lib/api/user.ts` — `fetchMyProfile`, `updateMyProfile` API 함수

## Components

- `components/my/MyPageContent.tsx` — 마이 페이지 조립 서버 컴포넌트; MyProfileCard·MyActivityStats·MyPostsPreview·MyCommentsPreview·MyAccountSection 레이아웃 구성
- `components/my/MyProfileCard.tsx` — 프로필 카드 + 닉네임/MBTI 편집 다이얼로그; useMyProfileSuspense 사용, 자체 QueryResetErrorBoundary+Suspense 포함
- `components/my/MyActivityStats.tsx` — 작성 게시글·댓글 수·최근 검사 MBTI 활동 통계; useMyActivityStatsSuspense 사용, 자체 QueryResetErrorBoundary+Suspense 포함
- `components/my/MyPostsPreview.tsx` — 내가 작성한 게시글 프리뷰 섹션 (placeholder)
- `components/my/MyCommentsPreview.tsx` — 내가 작성한 댓글 프리뷰 섹션 (placeholder, MyRecentlyViewedPreview로 교체됨)
- `components/my/MyRecentlyViewedPreview.tsx` — 최근 조회한 게시글 프리뷰 섹션; localStorage(recently_viewed_posts)에서 최대 5개 읽어 링크 목록 표시, useState lazy initializer로 클라이언트 마운트 시 로드
- `components/my/MyPostPreviewItem.tsx` — 마이페이지 게시글 목록 공용 행 컴포넌트; typeCode 뱃지·제목·시간 레이블 표시, stats prop(viewCount/commentCount) 선택적 제공 시 우측에 통계 표시; MyPostsPreview·MyRecentlyViewedPreview에서 공유
- `components/my/MyAccountSection.tsx` — 회원 탈퇴 섹션 + 2단계 OTP 다이얼로그; mutation 전용, Suspense 불필요

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

- `app/(main)/mbti/test/page.tsx` — `/mbti/test` 클라이언트 컴포넌트; useQueryErrorResetBoundary + ApiErrorBoundary + Suspense(MbtiTestLoading) 조합으로 데이터 fetch 에러·로딩 처리
- `app/(main)/mbti/test/MbtiTestContent.tsx` — MBTI 검사 클라이언트 컴포넌트; 문항 스텝 UI·진행 바·5지선다 채점·결과 카드·저장 모달, 비로그인 시 sessionStorage 점수 임시 저장 후 로그인 복귀 자동 제출, 재검사 시 캐시 무효화로 새 문항 랜덤 추출 (useSuspenseQuery 사용)
- `app/(main)/mbti/test/loading.tsx` — `/mbti/test` 라우트 이동 시 표시되는 Next.js route-level 로딩 UI; LuSnail 아이콘 bounce 애니메이션

---

# Phase 6-6 — MBTI 유형 목록 페이지

## Types

- `types/mbti.ts` — `MBTI_GROUPS` 상수 추가: 4개 기질 그룹(분석가/외교관/관리자/탐험가)별 MBTI ID 배열

## Components

- `components/mbti/MbtiTypeCard.tsx` — 단일 MBTI 유형 카드; iconUrl 이미지(없으면 colorCode 원형 폴백), 유형 코드(id), 요약(summary) 표시, 상단 colorCode 테마 보더

## Components

- `components/mbti/GardenProgress.tsx` — 흙밭(ground.png) 위에 채소(vegetable.png)를 clip-path로 좌→우 점진 노출하는 정원 진행도 컴포넌트; `progress`(0–4)에 따라 애니메이션으로 풍성해짐
- `components/mbti/MbtiDetail.tsx` — MBTI 유형 공통 상세 뷰; 이미지·설명·성격 특성·궁합 파트너 표시, Web Share API + 클립보드 폴백 공유 버튼 내장, `actionSlot` prop으로 저장 버튼 등 추가 액션 주입 가능

## Pages

- `app/(main)/mbti/page.tsx` — `/mbti` 서버 래퍼; MbtiListContent 렌더링
- `app/(main)/mbti/MbtiListContent.tsx` — MBTI 유형 목록 클라이언트 컴포넌트; useMbtiList로 전체 조회 후 MBTI_GROUPS 기준 4개 섹션 그리드, 로딩 스켈레톤, NONE 필터링, 검사 CTA 버튼, 카드 클릭 시 `/mbti/[id]` 상세 페이지 이동
- `app/(main)/mbti/[id]/page.tsx` — `/mbti/[id]` 서버 래퍼; params.id를 MbtiDetailContent에 전달
- `app/(main)/mbti/[id]/MbtiDetailContent.tsx` — MBTI 유형 상세 클라이언트 컴포넌트; useMbtiList 캐시에서 해당 유형 조회, 없으면 notFound(), MbtiDetail 렌더링

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

- `components/admin/AdminNav.tsx` — 어드민 전용 사이드바; 대시보드·유저·게시글·댓글·MBTI 문제 탭 링크, 데스크탑 sidebar + 모바일 드로어(navStore 공유), "일반 사이트로" 복귀 링크
- `components/admin/AdminDashboardContent.tsx` — 대시보드 클라이언트 컴포넌트; 총 유저·게시글·댓글 수 통계 카드(useAdminDashboardStats), 빠른 이동 그리드
- `components/admin/AdminUserTable.tsx` — 유저 관리 테이블; 이메일·상태·역할 필터, 페이지네이션, 행 클릭 시 AdminUserDrawer 오픈, 유저 추가 버튼으로 AdminCreateUserModal 오픈
- `components/admin/AdminUserDrawer.tsx` — 유저 상세/수정 사이드 드로어; 닉네임·프로필사진 삭제(updateProfile), 역할·상태 즉시 변경
- `components/admin/AdminCreateUserModal.tsx` — 어드민 유저 직접 생성 모달; 이메일·비밀번호·닉네임·MBTI·역할·상태 입력, 이메일 인증 없이 즉시 생성(useCreateAdminUser)
- `components/admin/AdminPostTable.tsx` — 게시글 관리 테이블; 유형·가시성·작성자·정렬 필터, 행 클릭 시 새 탭으로 게시글 페이지 이동, 인라인 숨기기/복구·삭제(2단계 확인) 버튼, 페이지네이션
- `components/admin/AdminCommentTable.tsx` — 댓글 관리 테이블; 게시글ID·가시성 필터, 숨김/복구 버튼(useMutation), 페이지네이션
- `components/admin/AdminMbtiContent.tsx` — MBTI 문항 관리 클라이언트 컴포넌트; AdminMbtiQuestion[] 직접 사용(타입 필터링 불필요), 행 클릭 시 모달 오픈, 노출 열에 AdminMbtiVisibilityButton 삽입
- `components/admin/AdminMbtiQuestionDetailModal.tsx` — MBTI 문항 상세 보기·수정 모달; AdminMbtiQuestion 타입 사용, title·category·isReverse 편집, isVisible 토글, isDirty 체크로 저장 버튼 비활성
- `components/admin/AdminMbtiVisibilityButton.tsx` — MBTI 문항 행별 독립 useMutation 인스턴스를 가진 노출 토글 버튼; 공유 mutation 경쟁 상태(race condition) 방지
- `components/admin/AdminFunVisibilityButton.tsx` — FUN 문항 행별 독립 useMutation 인스턴스를 가진 노출 토글 버튼; AdminFunVisibilityButton과 동일 패턴

## Layout

- `app/(admin)/layout.tsx` — 어드민 전용 레이아웃; Header + AdminNav 사이드바 + main, (main) 레이아웃의 SiteNav 대신 AdminNav 사용

## Middleware

- `middleware.ts` — /admin/:path* 경로: session 쿠키 없으면 /login 리다이렉트 (ADMIN 권한 검증은 API 서버에서 수행)

## Pages

- `app/(admin)/admin/page.tsx` — `/admin` 대시보드; AdminDashboardContent 렌더링
- `app/(admin)/admin/users/page.tsx` — `/admin/users` 유저 관리 페이지
- `app/(admin)/admin/posts/page.tsx` — `/admin/posts` 게시글 관리 페이지
- `app/(admin)/admin/comments/page.tsx` — `/admin/comments` 댓글 관리 페이지
- `app/(admin)/admin/mbti/page.tsx` — `/admin/mbti` MBTI 문제 관리 페이지

---

# Phase 8 (8-4 / 8-5) — 회원 정보 찾기

## Types

- `types/auth.ts` — `FindEmailRequest`, `FindEmailResponse`, `PasswordResetRequest` 타입 추가

## Lib

- `lib/apiEndpoints.ts` — auth 섹션에 findEmail, passwordResetRequest 엔드포인트 상수 추가
- `lib/routes.ts` — `ROUTES.accountFind` 추가: "/account/find"
- `lib/api/auth.ts` — `findEmail`, `requestPasswordReset` API 함수 추가

## Components

- `components/auth/FindAccountContent.tsx` — 이메일/비밀번호 찾기 탭 컨테이너; 탭 전환·헤더 config·passwordSent 상태 관리, FindEmailSection·FindPasswordSection 렌더링
- `components/auth/FindEmailSection.tsx` — 전화번호 입력으로 이메일 조회하는 섹션; usePhoneInput 훅·findEmail mutation·마스킹 결과 표시
- `components/auth/FindPasswordSection.tsx` — 이메일 입력으로 비밀번호 재설정 링크 발송하는 섹션; 2단계(입력 → 발송완료) 상태 관리
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
- `components/post/DebateProgressBar.tsx` — 토론 찬반 비율 그라디언트 프로그레스 바; `proPct·conPct` prop, `height` prop(기본 h-4)으로 크기 조절 가능
- `components/post/VoteSection.tsx` — 투표 결과 바·선택지·투표하기 버튼 통합 섹션; `postId·voteResult·isLoggedIn` prop, 내부에서 선택 상태·투표 변경·mutation 관리
- `components/post/DebateSection.tsx` — 토론 찬반 비율·진영 버튼·마감일 통합 섹션; `postId·debateResult·isLoggedIn·isEditing·onStanceChange` prop, 내부에서 selectedStance·stanceMutation 관리, 변경 시 onStanceChange로 부모(CommentSection)에 동기화

---

## Components (공통)

- `components/common/ShareButton.tsx` — 공유 버튼 컴포넌트; `path` prop으로 상대 경로를 받아 모바일은 Web Share API, 데스크탑은 URL 팝오버(단축 URL + 복사 버튼) 방식으로 공유



- `components/common/Logo.tsx` — `/images/logo.png` 이미지 기반 로고 컴포넌트; `size`(sm/md/lg)·`asLink`(홈 링크 여부) prop 지원, 클릭 시 `/` 이동
- `components/auth/SocialLoginButton.tsx` — 소셜 로그인 버튼 공용 컴포넌트; `provider`(google)·`redirectTo` prop으로 OAuth2 리다이렉트 처리, 스타일·경로는 PROVIDER_CONFIG 상수로 관리 (카카오는 미구현으로 제거됨)
- `components/common/Toast.tsx` — 완전 커스텀 토스트 시스템; `ToastProvider`(루트 마운트), `toast.success/error/info/warning()` 모듈 레벨 싱글턴으로 훅 없이 어디서든 호출 가능, 슬라이드인 애니메이션·자동 닫힘·수동 닫기 지원
- `components/common/FloatingLabelInput.tsx` — 플로팅 라벨 input 베이스 컴포넌트; `rightSlot` prop으로 우측 버튼 주입, `hint` prop으로 포커스 시 힌트 텍스트 표시 지원
- `components/common/EmailInput.tsx` — FloatingLabelInput 기반 이메일 전용 래퍼; 메일 아이콘·type email·autoComplete email 고정, 자동완성 기능 확장 예정
- `components/common/PasswordInput.tsx` — FloatingLabelInput 기반 비밀번호 전용 래퍼; show/hide 토글 상태 내장
- `components/common/MbtiSelect.tsx` — `@radix-ui/react-select` 기반 MBTI 선택 드롭다운; MBTI_GROUPS 그룹화·NONE→"미설정" 변환·체크 아이콘 표시 지원; `value·onChange·disabled` prop
- `components/common/LabeledFilterBox.tsx` — 좌측 라벨 + 우측 children을 테두리로 감싸는 필터 래퍼 컴포넌트; `label·children` prop
- `components/common/ErrorMessage.tsx` — 폼 에러 메시지 공통 컴포넌트; 빨간 테두리·반투명 배경·아이콘 포함, `message` null/undefined 시 렌더링 안 함
- `components/post/BoardTypeNav.tsx` — 전체·자유글·투표글·토론글 탭 링크; usePathname으로 현재 경로에 따라 활성 탭 강조
- `hooks/usePhoneInput.ts` — 전화번호 입력 상태·포맷팅(숫자만 추출 + 하이픈 자동 삽입) 훅; `{ phone, handlePhoneChange }` 반환
- `hooks/useNicknameCheck.ts` — 닉네임 실시간 중복 체크 훅; 400ms 디바운스 후 GET /api/users/check-nickname 호출, `skipValue`로 본인 기존 닉네임은 체크 생략, `{ available, taken }` 반환 (FormStep·MyProfileCard 공용)
- `hooks/usePostEditDelete.tsx` — 자유글 전용 수정/삭제 훅; 편집 상태·모달 상태·update/delete mutation·핸들러·menuItems·guestModalProps 반환, redirectPath로 삭제 후 이동 경로 주입
- `hooks/usePostDelete.tsx` — 투표·토론 전용 삭제 전용 훅; 수정 시 toast 표시(editUnavailableMessage), 삭제 시 게스트 비밀번호 모달 지원, menuItems·guestModalProps 반환
- `hooks/useImageUpload.ts` — 이미지 업로드 추적 훅; 업로드 URL Set 관리, 언마운트 시 미저장 이미지 PENDING 취소 요청(keepalive), `{ hasImage, handleUpload, markSaved }` 반환
- `components/post/PostDetailSkeleton.tsx` — 투표·토론 상세 페이지 공통 로딩 스켈레톤 (max-w-225)
- `components/post/PostAuthorMeta.tsx` — 상세 페이지 공통 작성자 행; MbtiBadge·닉네임·날짜·조회수 표시
- `components/post/PostHeader.tsx` — 상세 페이지 공통 헤더; 제목(수정 시 input 전환)·wanted 뱃지·케밥 메뉴·PostAuthorMeta 포함; menuItems prop으로 아이템 주입
- `components/post/PostMenuButton.tsx` — 상세 페이지 공통 케밥 메뉴 버튼; PostMenuItem 배열로 아이템 주입, fixed 오버레이로 외부 클릭 닫기
- `components/auth/BackToLoginLink.tsx` — "로그인으로 돌아가기" 링크 공용 컴포넌트; ROUTES.login으로 이동, 화살표 아이콘 포함
- `components/common/ValidationCheckBox.tsx` — 입력값 유효성 상태 표시 컴포넌트; `checks: CheckItem | CheckItem[]` prop으로 단일·복수 검증 항목 수용, valid 시 연두색 체크, invalid 시 주황색 X 표시; `CheckItem` 타입 named export
- `components/common/InfoBanner.tsx` — 안내 배너 공통 컴포넌트; 단건(`variant + text`)·복수(`items[]`) union props 지원, variant(info/warning/error/success)에 따라 아이콘·색상 자동 결정, 단건 시 rounded-full·복수 시 rounded-xl 렌더링
- `components/auth/AuthCardIllustration.tsx` — 인증 카드 삽화 헤더; h-44 배경에 w-24 원형 아이콘·선택적 뱃지, SignupContent EMAIL_SENT 단계에서 사용
- `components/auth/AuthCardHeader.tsx` — 인증 카드 상단 배경 헤더; `bg-secondary-container/20`에 w-16 원형 아이콘·선택적 StepIndicator, LoginForm·SignupContent EMAIL 단계에서 공용 사용
- `components/common/IconDivider.tsx` — 아이콘을 가운데에 둔 수평 구분선 컴포넌트; `icon`·`iconSize`·`className` prop 지원
- `components/common/TextDivider.tsx` — 텍스트를 가운데에 둔 수평 구분선 컴포넌트; `text`·`textSize`(xs/sm/md)·`color`(muted/default/primary)·`className` prop 지원

---

## SignupContent step 분리

- `components/auth/steps/EmailStep.tsx` — 회원가입 1단계 이메일 입력 카드; 마지막 로그인 안내 배너·소셜 로그인·이메일 폼 포함
- `components/auth/steps/EmailSentStep.tsx` — 회원가입 2단계 이메일 발송 대기 카드; 재발송 쿨다운 카운트다운·뒤로가기 버튼 포함
- `components/auth/steps/FormStep.tsx` — 회원가입 3단계 프로필 입력 폼; 닉네임·비밀번호·MBTI·전화번호 입력, 클라이언트 유효성 검증 내장
- `components/auth/steps/DoneStep.tsx` — 회원가입 완료 카드; 로그인 이동 링크 표시

---

# Phase 10 — 모바일 네비게이션

## Constants

- `constants/navigation.ts` — BOARD_TABS·MY_TABS·EXTRA_TABS 네비게이션 상수; Sidebar·MobileDrawer가 공유

## Components

- `components/common/MobileDrawer.tsx` — 모바일 전용 좌측 슬라이드 드로어; pathname 기반 게시판/마이페이지 탭 전환, 백드롭 클릭으로 닫기, useSearchParams로 현재 탭 활성 상태 표시

---

# Phase 10-2 — 계정 관리 페이지

## Constants

- `constants/navigation.ts` — MY_TABS에 `계정 관리 → /my/account` 탭 추가 (MdManageAccounts 아이콘)
- `constants/routes.ts` — ROUTES.myAccount `/my/account` 추가
- `constants/apiEndpoints.ts` — users 섹션에 `mePhone: "/api/users/me/phone"` 추가

## Types

- `types/user.ts` — MyProfileResponse에 `hasPhone: boolean` 추가; ChangePasswordRequest 인터페이스 추가

## Lib

- `lib/api/user.ts` — `changePassword`, `updatePhone`, `removePhone` API 함수 추가

## Components

- `components/my/account/MyAccountContent.tsx` — 계정 관리 페이지 조립 컴포넌트; useMyProfileSuspense로 isSocialOnly·hasPhone 읽어 각 섹션에 전달, QueryResetErrorBoundary+Suspense 포함
- `components/my/account/MyPasswordSection.tsx` — 비밀번호 변경 섹션; 현재/새 비밀번호 입력, ValidationCheckBox로 정책 실시간 표시, isSocialOnly=true 시 InfoBanner로 비활성 안내
- `components/my/account/MyPhoneSection.tsx` — 전화번호 관리 섹션; hasPhone 상태 배지, 등록/변경/삭제 다이얼로그, 등록 시 중복 검증, invalidateQueries로 프로필 캐시 갱신
- `components/my/account/MyWithdrawalSection.tsx` — 회원 탈퇴 섹션 (MyAccountSection에서 이전); 2단계 OTP 다이얼로그

## Pages

- `app/(main)/my/account/page.tsx` — `/my/account` 서버 컴포넌트; MyAccountContent 렌더링

---

# Refactor — 통합 작성 페이지 분리

## Hooks

- `hooks/useWriteForm.ts` — 통합 작성 폼 커스텀 훅; 공통 필드(title/content/postType)·타입별 mutation 3종·`handleSubmit` 분기를 관리하고 아래 두 sub-hook을 조합
- `hooks/useVoteOptionsForm.ts` — 투표 선택지 입력 상태(직접 입력/MBTI 모드 토글); `OptionRow` 타입 export (VOTE 전용, useWriteForm에서 분리)
- `hooks/useDeadlineForm.ts` — VOTE/DEBATE 공용 마감일 입력 상태("기한 없음" 토글 + 유효성 검증); useWriteForm에서 분리

## Components (Write 전용)

- `components/post/DeadlineField.tsx` — 마감일 datetime-local 입력 + "기한 없음" 토글 버튼 공통 컴포넌트; VoteOptionsField·DebateLabelsField에서 공유
- `components/post/VoteOptionsField.tsx` — 투표 선택지 fieldset + DeadlineField 조합 컴포넌트; MBTI 모드 토글·항목 추가/삭제/입력 UI
- `components/post/DebateLabelsField.tsx` — 토론 의견1/2 레이블 입력 + DeadlineField 조합 컴포넌트

## Pages (Write)

- `app/(main)/board/write/WriteFormContent.tsx` — useWriteForm 훅으로 로직 분리 후 순수 UI 렌더링만 담당하도록 리팩토링

---

# Refactor — 통합 게시판 목록 컴포넌트 분리

## Components

- `components/post/PostCardSkeleton.tsx` — 게시글 카드 로딩 스켈레톤; BoardPostList Suspense fallback 및 무한 스크롤 로딩 인디케이터로 공용 사용
- `components/post/BoardPostList.tsx` — 게시판 목록 UI 전담 클라이언트 컴포넌트; useSuspensePostList 기반, 카드/리스트 뷰 모드·무한 스크롤·광고 삽입 담당, type·sort·authorMbti·viewMode는 BoardView로부터 props로 전달받음 (isLoading/isError 제거, Suspense+QueryResetErrorBoundary로 위임)
- `components/post/BoardListHeader.tsx` — 게시판 제목/설명 + 정렬·뷰모드·MBTI 필터 헤더; BoardPostList의 Suspense 경계 밖에 위치해 데이터 재요청 중에도 깜빡이지 않음, 상태는 상위(BoardView)에서 props로 주입
- `app/(main)/board/BoardView.tsx` — sort·authorMbti·viewMode state를 보유하는 얇은 래퍼; BoardListHeader + Suspense(BoardPostList) 조합, BoardPageClient에서 board 타입 변경 시 key로 리마운트되어 필터 상태 초기화
- `components/post/PostCardShared.tsx` — PostCard/PostListItem 공용 서브 컴포넌트(DebateBar, VoteOptionList)와 `PostCardBaseProps` 타입 모음
- `components/post/PostCard.tsx` — 카드 뷰 전용으로 분리(기존 `card` prop 분기 제거); 헤더+제목+통계 풀 레이아웃
- `components/post/PostListItem.tsx` — 목록 뷰 전용 컴포넌트(기존 PostCard의 list 분기를 분리); 가로 한 줄 레이아웃, 썸네일 80x80
- `components/mbti/MbtiTypePreviewCard.tsx` — MbtiBadge 팝오버용 MBTI 유형 미리보기 카드; MbtiAvatar(lg) + 유형 코드 + personalities 아이콘·라벨 4개 표시

---

- `components/common/ViewModeToggle.tsx` — 카드/리스트 뷰 모드 전환 토글 버튼; `viewMode`·`onChange` prop, VIEW_MODE/ViewMode는 @/types/post에서 공유

---

# Phase FN — Fun Question 통계 페이지

## Pages

- `app/(main)/mbti/fun/stats/page.tsx` — `/mbti/fun/stats` 서버 컴포넌트; QueryResetErrorBoundary + Suspense(FunStatsLoading 스켈레톤) 조합으로 FunStatsContent 감싸기
- `app/(main)/mbti/fun/stats/FunStatsContent.tsx` — Fun 질문 통계 클라이언트 컴포넌트; useFunStats로 전체 질문·선택지·MBTI별 집계 조회, 질문별 옵션 바(최댓값 기준 정규화) + MBTI 뱃지 표시

---

# Phase — 어드민 유저 통계

## Components

- `components/admin/AdminUserStatsContent.tsx` — 유저 통계 대시보드 클라이언트 컴포넌트; Recharts PieChart(상태·역할·MBTI인증·로그인방식)×4 + BarChart(MBTI분포) + AreaChart(30일 가입 추이), useAdminUserStats hook 사용

## Pages

- `app/(admin)/admin/users/stats/page.tsx` — `/admin/users/stats` 유저 통계 페이지; ApiErrorBoundary + Suspense + AdminUserStatsContent

---

# Phase — 어드민 게시글/댓글/MBTI 검사 통계

## Components

- `components/admin/AdminPostStatsContent.tsx` — 게시글 통계 대시보드; 총 게시글·숨김 현황 카드, 타입 분포 progress bar, AreaChart(30일 추이), BarChart(MBTI별 작성 분포)
- `components/admin/AdminCommentStatsContent.tsx` — 댓글 통계 대시보드; 총 댓글·숨김 현황 카드, 좋아요/싫어요 리액션 현황, AreaChart(30일 추이), BarChart(MBTI별 댓글 분포)
- `components/admin/AdminMbtiTestStatsContent.tsx` — MBTI 검사 통계 대시보드; 누적 검사 횟수 카드, 지표별 평균 progress bar, 재검사 빈도 progress bar, AreaChart(30일 추이), BarChart(검사 결과 MBTI 분포)

## Pages

- `app/(admin)/admin/posts/stats/page.tsx` — `/admin/posts/stats` 게시글 통계 페이지
- `app/(admin)/admin/comments/stats/page.tsx` — `/admin/comments/stats` 댓글 통계 페이지
- `app/(admin)/admin/mbti/stats/page.tsx` — `/admin/mbti/stats` MBTI 검사 통계 페이지

## Legal Pages

- `components/common/Footer.tsx` — 사이트 하단 푸터; 이용약관·개인정보처리방침 링크 포함, 모든 레이아웃에서 공유
- `app/(main)/terms/page.tsx` — `/terms` 이용약관 정적 페이지
- `app/(main)/privacy/page.tsx` — `/privacy` 개인정보처리방침 정적 페이지

---

## Refactor — MBTI 검사 선택지 버튼 분리

- `components/mbti/OptionButton.tsx` — 라디오 스타일 선택지 버튼 공용 컴포넌트; `isSelected`·`onClick`·`children` prop, FUN 문항(optionText)과 일반 문항(label+subLabel) 모두 공유

## Admin Stats 캐시 갱신 버튼 컴포넌트

- `components/admin/StatsRefreshButton.tsx` — 어드민 통계 캐시 수동 갱신 버튼; `isRefreshing`·`onRefresh` prop, 갱신 중 스피너+비활성화 처리, 4개 StatsContent에서 공유

## Refactor — Admin Stats 4개 컴포넌트 중복 제거

- `lib/adminStatsUtils.ts` — `formatTrendDate`(일별/주별 날짜 포맷)·`chunkWeekly`(7일 단위 주별 집계)·`computeGrowth`(전월 대비 증감률)·`withPercentLabel`(항목별 비율 라벨 부착) 공용 유틸 + `CHART_AXIS_COLOR`/`CHART_GRID_COLOR` 차트 색상 상수; 4개 Admin StatsContent에서 중복 정의되던 로직 통합
- `components/admin/GrowthBadge.tsx` — 전월 대비 증감 배지(▲▼ 아이콘 + 퍼센트); `diff`·`pct` prop, pct null이면 미표시
- `components/admin/TrendAreaChart.tsx` — 일별/주별 토글 + Recharts AreaChart 카드 공용 컴포넌트; `title`·`data`·`trendMode`·`onTrendModeChange`·`color`·`gradientId`·`unitLabel` prop (Comment/Post/MbtiTest 통계에서 공유)
- `components/admin/MbtiDistributionChart.tsx` — MBTI별 분포 Recharts BarChart 카드 공용 컴포넌트; `title`·`data`(`{name,value,label}[]`)·`unitLabel` prop, data 비어있으면 렌더링 안 함 (4개 StatsContent 전체에서 공유)

## 공지사항 기능

- `types/notice.ts` — `NoticeResponse`·`AdminNoticeResponse`·`NoticeCreateRequest`·`NoticeUpdateRequest` 타입 + `NOTICE_HIDDEN_KEY` 상수
- `lib/api/notice.ts` — `fetchActiveNotices`(공개)·`fetchAdminNotices`·`createAdminNotice`·`updateAdminNotice`·`deleteAdminNotice` API 함수
- `hooks/useNotice.ts` — localStorage `notice_hidden_until` 판별 + useQuery 조합 훅; `isVisible`·`close`·`hideFor24Hours` 반환
- `components/common/NoticeModal.tsx` — 공지사항 캐러셀 팝업 모달; 좌우 화살표·dot indicator·"24시간 동안 보지 않기" 버튼; (main)/layout에서 마운트
- `components/admin/AdminNoticeContent.tsx` — 어드민 공지사항 CRUD 관리 페이지; 목록 조회·등록·수정(인라인 폼 모달)·삭제(확인 버튼) 포함

## Refactor — Suspense fallback 로딩 텍스트 공용화

- `components/common/LoadingIndicator.tsx` — Suspense fallback 공용 로딩 텍스트; "..." 점 3개 순차 bounce 애니메이션, `message`·`className` prop으로 커스터마이즈, 반복되던 "불러오는 중..." p 태그 12곳 대체

## 소셜 로그인 약관 동의 게이트 (dev_phase.md)

- `constants/routes.ts` — `ROUTES.termsAgree: "/terms/agree"` 추가
- `constants/apiEndpoints.ts` — `API.auth.oauth2Signup: "/api/auth/oauth2/signup"` 추가
- `types/auth.ts` — `SignupRequest`에 `termsAgreed`·`privacyAgreed` 추가, `OAuth2SignupConfirmRequest` 타입 신설
- `lib/api/auth.ts` — `confirmOAuth2Signup` API 함수 추가
- `components/auth/TermsAgreementCheckboxes.tsx` — 이용약관/개인정보처리방침 동의 체크박스 쌍 공용 컴포넌트; `FormStep.tsx`·`WriteFormContent.tsx`(비회원 글쓰기) 중복 제거
- `components/auth/TermsAgreementContent.tsx` — 약관 동의 페이지(라우트/이름을 "소셜 가입" 대신 범용화, 향후 재동의 흐름과 공유 목적); `useSyncExternalStore`로 URL fragment의 `signupToken`을 SSR 안전하게 읽음, 동의 완료 시 AT 저장+`setLastLoginMethod`+원래 목적지 리다이렉트
- `app/(auth)/terms/agree/page.tsx` — `/terms/agree` 서버 컴포넌트; `TermsAgreementContent` 렌더링

## 페이지네이션 공용 훅 + MyPostsSection 분리

- `hooks/usePagination.ts` — 페이지 번호 배열(`buildPageNumbers`, 생략 부호는 `PAGE_ELLIPSIS` 상수) + `goToPrev`/`goToNext` 핸들러 반환 공용 훅; `MyPostsList`·`AdminUserTable`·`AdminPostTable`·`AdminCommentTable`에서 중복 정의되던 로직 통합
- `components/my/MyPostsHeader.tsx` — 마이페이지 게시글 섹션 헤더(제목+총 개수) + 전체/자유글/투표/토론 필터 탭; `MyPostsSection`에서 분리된 presentational 컴포넌트
- `components/my/MyPostsList.tsx` — 게시글 목록(썸네일·뱃지·수정삭제) + 페이지네이션 nav; `usePagination` 사용, 빈 상태 안내 포함; `MyPostsSection`에서 분리된 presentational 컴포넌트
- `components/my/MyPostsSection.tsx` — 상태(`selectedType`·`page`)·쿼리·삭제 mutation만 소유하고 `MyPostsHeader`·`MyPostsList`를 조립하는 컨테이너로 축소
- `components/auth/OAuth2CallbackContent.tsx` — `signupToken` 분기 추가; 있으면 sessionStorage 정리 없이 `/terms/agree`로 이동(정리는 동의 완료 페이지가 이어받음)

## Refactor — MyProfileCard 편집 다이얼로그 분리

- `components/my/NicknameEditDialog.tsx` — 별명 변경 다이얼로그; `MyProfileCard`에서 분리된 presentational 컴포넌트, nickname·에러·중복확인 상태를 props로 수신
- `components/my/MbtiEditDialog.tsx` — MBTI 타입 변경 다이얼로그; `MyProfileCard`에서 분리된 presentational 컴포넌트, mbtiId·mutation 상태를 props로 수신

## Fix — SiteNav/Header 로그인 상태 하이드레이션 불일치

- `hooks/useHasMounted.ts` — `useSyncExternalStore` 기반 마운트 여부 훅; 서버 렌더·클라이언트 첫 하이드레이션은 항상 `false`, 커밋 이후 `true`로 전환 (`useIsDesktop`과 동일 패턴, `setState-in-effect` lint 위반 없이 hydration-safe)
- `components/common/SiteNav.tsx` — `NavContent`에서 `isLoggedIn` 조건에 `useHasMounted()` 게이트 추가; `AuthSyncProvider`의 silent refresh가 `<Suspense>`로 지연된 이 서브트리의 하이드레이션보다 먼저 끝나 발생하던 하이드레이션 실패 수정
- `components/common/Header.tsx` — 동일한 `useHasMounted()` 게이트를 로그인 버튼/마이페이지 아이콘 분기에 적용 (잠재적 하이드레이션 불일치 예방)

## 자동 로그인(rememberMe) + 연동 기기 전체 로그아웃

- `components/auth/LoginForm.tsx` — "자동 로그인" 체크박스 추가; 로그인 요청에 `rememberMe` 포함
- `store/authStore.ts` — `setAccessToken(token, rememberMe?)`으로 확장; `rememberMe=false`면 `session` 힌트 쿠키를 Max-Age 없는 브라우저 세션 쿠키로 발급(RT 쿠키와 동일 정책), `localStorage`에 선택값을 저장해 silent refresh 등 rememberMe를 모르는 호출에서 재사용
- `components/my/account/MyLogoutAllSection.tsx` — 마이페이지 계정관리에 "연동 기기 전체 로그아웃" 카드+확인 다이얼로그 추가; `POST /api/auth/logout/all` 호출 후 로컬 세션 정리 및 로그인 페이지 이동
- `components/my/account/MyAccountContent.tsx` — `MyLogoutAllSection` 렌더링 추가
- `components/my/MyAccountSection.tsx` — 삭제 (과거 리팩터링 때 `MyWithdrawalSection.tsx`로 로직이 이전된 뒤 어디서도 렌더링되지 않던 죽은 컴포넌트)

## 배포 전 정리 — 카카오 로그인 전체 제거

카카오 OAuth2 앱 등록이 안 된 상태로 배포하면 `application.yaml`의 `${KAKAO_CLIENT_ID}` placeholder 해석 실패로 서버 부팅 자체가 실패할 위험이 있어, 미구현 상태였던 카카오 로그인을 완전히 제거함(프론트 버튼은 이전부터 주석 처리되어 있었음).

- `components/auth/SocialLoginButton.tsx` — `PROVIDER_CONFIG`에서 `kakao` 항목 제거, `google`만 남음
- `components/auth/SocialLoginButtons.tsx` — 주석 처리되어 있던 카카오 버튼 블록 삭제
- `lib/loginUtils.ts` — `LOGIN_METHOD`/`LOGIN_METHOD_LABEL`에서 `KAKAO` 제거
- 백엔드 `application.yaml`/`application-test.yaml` — `spring.security.oauth2.client.registration.kakao`, `provider.kakao` 설정 블록 제거
- `global/security/oauth2/userinfo/KakaoOAuth2UserInfo.java` — 삭제
- `global/security/oauth2/CustomOAuth2UserService.java` — `resolveUserInfo()`의 `"kakao"` 분기 제거
- 루트 `.env` — 주석 처리되어 있던 `KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET` 제거
- `app/(main)/my/page.tsx`, `app/(main)/my/account/page.tsx`, `app/(main)/my/posts/page.tsx` — 클라이언트 전용 데이터(AT는 메모리에만 존재)를 서버에서 렌더링 시도하며 매 요청 401이 발생하던 문제 수정; `next/dynamic(..., { ssr: false })`로 콘텐츠를 클라이언트 전용 렌더링으로 전환(admin 페이지와 동일 패턴), `next/dynamic` import는 라우트 세그먼트 설정 `export const dynamic`과의 이름 충돌을 피하기 위해 `nextDynamic`으로 별칭 처리

## 배포 전 정리 — 죽은 컴포넌트 삭제

배포 전 불필요한 파일 점검 중 발견한, 리팩터링 후 어디서도 렌더링되지 않던 컴포넌트 4개 삭제 (`MyAccountSection.tsx`와 동일한 "로직만 옮겨가고 파일은 안 지워진" 패턴).

- `components/my/MyCommentsPreview.tsx` — 삭제 (`MyRecentlyViewedPreview.tsx`로 대체된 뒤 `MyPageContent.tsx`에서 더 이상 렌더링되지 않던 placeholder)
- `components/post/PostListFilter.tsx` — 삭제 (정렬·뷰모드·MBTI 필터 기능이 `BoardListHeader.tsx`로 이전됨)
- `components/common/ValidationCheckBox.tsx` — 삭제 (`MyPasswordSection.tsx`가 더 이상 사용하지 않음 — 문서엔 사용한다고 남아있었으나 실제 코드는 이미 다른 방식으로 리팩터링됨)
- `components/theme/ThemeSwitcher.tsx` — 삭제 (`Header`/`SiteNav` 어디에도 연결 안 됨; `app/layout.tsx`의 `next-themes` `ThemeProvider` 자체는 유지되어 OS 다크모드 설정은 그대로 반영됨, 수동 전환 버튼만 없었음)

## 핸드폰 번호 기능 전면 제거 → 인증된 복구 이메일로 대체

미인증 전화번호로 이메일 찾기를 하던 방식이 (1) 전화번호 열거로 가입 여부 조회 가능 (2) 타인 번호 입력 시 도용 가능한 문제가 있어, 회원가입 시 phone 입력을 완전히 제거하고 마이페이지에서 소유권 인증(발송+링크 확인)을 거친 복구 이메일로 대체함.

- `hooks/usePhoneInput.ts` — 삭제 (전화번호 포맷팅 훅, 모든 사용처 제거로 unused)
- `components/auth/steps/FormStep.tsx` — 회원가입 폼에서 전화번호 입력 필드·`usePhoneInput`·중복 검증 로직 제거
- `components/auth/SignupContent.tsx` — `isDuplicatePhone` 판별 로직 제거
- `components/auth/FindEmailSection.tsx` — 전화번호 입력 → 복구 이메일 입력으로 교체 (`MdEmail` 아이콘, `EMAIL_REGEX` 검증, `findEmail({ recoveryEmail })`)
- `components/auth/FindAccountContent.tsx` — 이메일 찾기 탭 안내 문구를 복구 이메일 기준으로 변경
- `components/my/account/MyPhoneSection.tsx` — 삭제 → `components/my/account/MyRecoveryEmailSection.tsx`로 대체; 즉시 저장 대신 인증 메일 발송(`sendRecoveryEmailVerification`) 후 링크 클릭으로 확정되는 흐름, 삭제는 기존과 동일한 즉시 반영 다이얼로그
- `components/my/account/RecoveryEmailVerifyContent.tsx` — `SignupVerifyContent.tsx`와 동일한 token 확인 패턴; 성공 시 프로필 캐시 무효화 후 `/my/account`로 리다이렉트
- `app/(main)/my/account/recovery-email/verify/page.tsx` — 위 컴포넌트를 렌더링하는 인증 링크 착지 페이지 (`searchParams.token` 전달)
- `components/my/account/MyAccountContent.tsx` — `MyPhoneSection` → `MyRecoveryEmailSection`, `hasRecoveryEmail` prop 전달
- `components/admin/AdminUserDrawer.tsx` — 전화번호 등록 여부 행 → 복구 이메일 등록 여부로 변경
- `components/admin/AdminUserStatsContent.tsx` — "전화번호 인증률" 카드 → "복구 이메일 등록률" 카드 (`recoveryEmailCount`/`recoveryEmailPct`, `MdAlternateEmail` 아이콘)
- `constants/apiEndpoints.ts` — `mePhone` 제거, `meRecoveryEmailSend`/`meRecoveryEmailConfirm`/`meRecoveryEmail` 추가
- `constants/routes.ts` — `myAccountRecoveryEmailVerify: "/my/account/recovery-email/verify"` 추가
- `types/auth.ts`, `types/user.ts`, `types/admin.ts` — `SignupRequest.phone` 제거, `FindEmailRequest.phone`→`recoveryEmail`, `hasPhone`→`hasRecoveryEmail`, `phoneVerifiedCount`→`recoveryEmailCount`, `RecoveryEmailSendRequest`/`RecoveryEmailConfirmRequest` 추가
- `lib/validationConstants.ts` — `PHONE_REGEX`/`PHONE_MESSAGE` 제거, `EMAIL_REGEX`/`EMAIL_MESSAGE` 추가
- `lib/api/user.ts` — `updatePhone`/`removePhone` → `sendRecoveryEmailVerification`/`confirmRecoveryEmailVerification`/`removeRecoveryEmail`
