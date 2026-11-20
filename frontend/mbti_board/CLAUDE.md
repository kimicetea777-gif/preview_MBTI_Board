@AGENTS.md

# MBTI Board (Frontend) - Claude Code Guide

이 저장소(`frontend/mbti_board`)에서 작업하는 Claude Code(및 다른 개발자)를 위한 안내입니다. 백엔드 규칙은 `../../backend/mbti_board/CLAUDE.md`.

## 프로젝트

MBTI 기반 커뮤니티 플랫폼. 자유글·투표·토론 게시판을 제공하며 회원/비회원 모두 이용 가능.

**로그인은 백엔드를 거쳐 인증 서버(AS)에 위임한다.** 브라우저는 HttpOnly 세션 쿠키(`session`)만 갖고, 토큰은 프론트에서 다루지 않는다.

## 문서

설계·운영 문서는 저장소 밖 `../../../Document/`에 있다 (git 추적 안 됨). 폴더 지도 문서는 아직 없다.
- `Failure and Success.md` — 구조가 왜 이렇게 됐는지 (AS 위임, Image Server 분리 등)
- `front_fn_design/componet_descript.md` — 컴포넌트 한 줄 요약 (**컴포넌트를 생성하면 여기에 추가**)
- `front_fn_design/test_case.md` — 프론트 테스트 케이스 명세
- `front_fn_design/security_descript.md` — 프론트 보안·세션 관련 설명
- `self_test.md` — 브라우저로 직접 확인하는 수동 체크리스트
- `build_manual.md` — 이미지 빌드 → SSH 전송 → 배포·롤백

작업 규칙:
- 큰 변경은 `Document/`에 계획 문서를 먼저 쓰고 그 단계대로 진행한다.
- 문서 파일 이름은 이동은 되지만 이름 변경은 인용하는 곳을 같이 고친다.
- 예전 CLAUDE.md가 가리키던 `prd.md`, `function_design/`은 현재 존재하지 않는다. API 계약의 기준은 백엔드 컨트롤러와 `types/*.ts`다.

## 스택

- Next.js 16.2 (App Router, `proxy.ts`) + React 19 + TypeScript
- Tailwind CSS v4 + shadcn/ui (Radix), `next-themes`(다크 모드)
- TanStack Query v5 (서버 상태), Zustand (`store/authStore` 등 클라이언트 상태), axios (`lib/apiClient.ts`)
- Tiptap 3 (에디터: 이미지·유튜브), Recharts (어드민 차트), react-icons
- 배포: `output: "standalone"` → Docker 이미지 → Cloudflare Tunnel

`next-intl`은 `package.json`에 있지만 **코드에서 쓰이지 않는다.** 화면 문구는 현재 코드에 직접 들어 있다. i18n을 도입하기 전까지는 문구를 새로 흩뿌리지 않도록 한 곳(컴포넌트 상단 상수 등)에 모은다.

## 명령어

`frontend/mbti_board`에서 실행한다.

- `npm run dev` — 개발 서버 (`next dev -H 0.0.0.0`, 기본 포트 3000). 백엔드(8080)가 떠 있어야 한다.
- `npm run lint` — ESLint
- `npx tsc --noEmit` — 타입 체크. 오류가 `.next/dev/types/`에서만 나면 아래 "겪은 버그" 3번을 본다.
- `npm run build` — 프로덕션 빌드
- 테스트 스크립트는 없다 (`test_case.md`는 명세일 뿐이다). 로그인·권한 흐름은 `Document/self_test.md`로 브라우저에서 확인한다.
- 배포: 루트에서 `docker compose build backend frontend` → `Document/build_manual.md`

## 핵심 원칙 (반드시 지킬 것)

1. **하드코딩 금지 — 경로·엔드포인트·도메인 상수**
   - URL 경로는 `constants/routes.ts`의 `ROUTES` 상수만 참조한다.
   - API 엔드포인트는 `constants/apiEndpoints.ts`의 `API` 상수만 참조한다. 컴포넌트마다 경로 문자열을 쓰지 않는다.
   - 도메인 상수(숫자 리터럴, enum 값 등)는 해당 `types/*.ts`에 상수로 정의해서 쓴다. 비교·분기 문자열과 요청 body의 enum 필드도 포함한다.
     - 예: `=== "FREE"` → `=== POST_TYPE.FREE`, `type: "FREE"` → `type: POST_TYPE.FREE`
     - 정의 형태: `export const FOO = { A: "A", B: "B" } as const;`
   - 공통 유틸(날짜 포맷 등)은 `lib/` 아래 전용 파일에 둔다 (예: `lib/dateUtils.ts`). 같은 함수를 두 컴포넌트에 각각 선언하지 않는다.
2. **API 키·시크릿은 서버에만 둔다**: `NEXT_PUBLIC_`을 붙인 값은 브라우저 번들에 들어간다. 시크릿에 절대 붙이지 않는다.
3. **인증 상태는 서버가 진실이다**: `proxy.ts`는 `session` 쿠키 **유무**만 보고 `/my`, `/admin`, `/board/write?type=VOTE|DEBATE`를 막는다. 쿠키가 유효한지, ADMIN 권한이 있는지는 API 서버가 판단하므로 프론트에서 권한을 믿고 데이터를 노출하지 않는다.

## 코드 컨벤션

### 데이터 페칭

- `useQuery` 대신 `useSuspenseQuery`를 쓴다.
- 컴포넌트 안에서 `isLoading` / `isError`로 분기하지 않는다. 페이지(또는 상위 래퍼)에서 `<Suspense fallback={...}>` + `<ApiErrorBoundary>`로 감싼다.
- 페이지 일부만 따로 로딩·실패해야 하면 그 컴포넌트 안에서 감싼다. 예: `CommentSection`은 안쪽 `CommentList`를 `<QueryResetErrorBoundary>` + `<Suspense fallback={<CommentSkeleton />}>`로 감싸서, 댓글이 실패해도 본문은 보이고 "다시 시도"로 쿼리를 다시 부른다.
- **예외 (`useQuery` 유지)**: `enabled`로 조건부 요청하고 실패해도 화면을 막으면 안 되는 보조 데이터만. 현재 `hooks/useNotice.ts`(공지 팝업)와 `components/post/VoteSection.tsx`(MBTI별 통계 토글) 두 곳이고, 각 자리에 이유 주석이 있다. 예외를 늘릴 때도 이유 주석을 단다.
- 모든 호출은 `lib/apiClient.ts`(axios 인스턴스, `withCredentials: true`)를 거친다. `lib/api/*.ts`에 도메인별 호출 함수를 둔다.

### 요청 제한시간

- fetch/axios는 기본으로 제한시간이 없다. 모든 요청에 제한시간을 두고, 값은 `constants/timeouts.ts`에만 둔다.
  - `API_TIMEOUT_MS`(10초) — `apiClient` 기본값. 초과하면 `timeoutErrorMessage`의 한국어 문구로 reject되어 `ApiErrorBoundary`가 그대로 보여 준다.
  - `IMAGE_UPLOAD_TIMEOUT_MS`(30초) — 브라우저 → `/api/upload` (`lib/api/image.ts`, `AbortSignal.timeout`)
  - `IMAGE_SERVER_TIMEOUT_MS`(25초) — `/api/upload` → Image Server. 브라우저 쪽보다 짧게 둬서 라우트가 먼저 504를 돌려준다.
  - `POST_META_TIMEOUT_MS`(3초) — 서버 렌더링 `lib/postMetadata.ts`(게시글 상세 `generateMetadata`). 초과하면 기본 metadata로 렌더링한다.
  - `SITEMAP_FETCH_TIMEOUT_MS`(10초) — `app/sitemap.ts`의 백엔드 페이지 하나당. 초과하면 그때까지 모은 글로 sitemap을 만든다.
- 새로 `fetch`를 쓰면 `signal: AbortSignal.timeout(...)`을 같이 넣는다. 오래 걸리는 호출만 상수를 따로 만든다.
- 서버 컴포넌트·`generateMetadata`의 `fetch`에 `signal`을 넣으면 같은 렌더 안의 요청 합치기(memoization)가 꺼진다. `revalidate` 데이터 캐시는 유지된다. 한 렌더에서 같은 URL을 여러 번 부르는 곳이면 이 점을 고려한다.

### 에러 문구

- `ApiErrorBoundary`의 문구는 `lib/apiError.ts`의 `getErrorDisplayMessage`가 정한다: 서버 `message` → 시간 초과 문구 → 연결 실패 문구 → 일반 문구. axios·JS의 영어 `error.message`를 화면에 그대로 내보내지 않는다.
- 토스트용 `getApiErrorMessage(error, fallback)`는 서버 `message`가 없으면 호출부의 `fallback`을 쓴다 (따로 둔 함수다).

### 인증과 401

- `apiClient`는 401을 받으면 재시도 없이 로그아웃 상태로 바꾸고(`useAuthStore`), 다른 탭에 알리고(`BroadcastChannel`, `lib/authChannel.ts`), 로그인 페이지로 보낸다. 서버가 이미 세션 갱신을 시도한 뒤의 401이기 때문이다. 프론트에 토큰 재발급 로직을 다시 만들지 않는다.
- 로그인 후 돌아갈 주소(`redirect` 쿼리)는 `lib/safeRedirect.ts`를 거쳐 쓴다 (`components/auth/`에서 사용 중).

### 마크업·디자인

- 본문은 `<article>`, `<section>`, `<p>`, 메뉴는 `<nav>`, 하단 정보는 `<footer>`처럼 의미 있는 태그를 쓴다.
- 아이콘은 `react-icons`로 통일한다.
- 색·간격은 Tailwind 토큰(`app/globals.css`)과 shadcn 컴포넌트를 쓰고 컴포넌트에 색 값을 직접 쓰지 않는다. 다크 모드에서도 확인한다.
- 흰 글자를 올리는 채운 배경은 대비 4.5:1 이상인 색만 쓴다.

### Next.js

- 설치된 Next.js(16)는 학습 데이터와 API가 다를 수 있다. 코드를 쓰기 전에 `node_modules/next/dist/docs/`의 해당 가이드를 읽는다. `middleware`는 `proxy`로 이름이 바뀌었다 (`proxy.ts`).
- `"use client"` 파일에서는 `metadata`를 export할 수 없다. 서버 `page.tsx`(metadata·`generateMetadata`)와 클라이언트 컴포넌트로 나눈다.
- `NEXT_PUBLIC_*`는 **빌드 시점에 번들에 박힌다.** 값을 바꾸려면 이미지를 다시 빌드해야 하고 `.env_next` 수정만으로는 반영되지 않는다. (`NEXT_PUBLIC_API_URL`은 `lib/apiClient.ts`에서 읽고 없으면 `http://localhost:8080`)
- 외부 이미지는 `next.config.ts`의 `images.remotePatterns`에 등록된 도메인(Image Server의 R2 공개 도메인)만 허용된다. 도메인이 바뀌면 여기를 같이 고친다.
- 보안 헤더(`X-Frame-Options`, CSP `frame-ancestors`, `Permissions-Policy` 등)는 `next.config.ts`에서 건다. 줄이기 전에 이유 주석을 읽는다.
- SEO: 게시글·MBTI 상세는 `generateMetadata`, 사이트 전체는 `app/sitemap.ts`, `app/robots.ts`. 게시판 구 경로(`/board/free` 등)는 `permanent` 리다이렉트다.
- 라우트 그룹: `(main)` 공개 화면, `(auth)` 로그인, `(admin)` 관리자. 관리자 스타일이 공개 화면에 섞이지 않게 한다.

### 에디터·이미지

- 이미지 업로드는 브라우저 → `app/api/upload/route.ts`(Next 서버 라우트) → Image Server(`IMAGE_SERVER_URL`, `X-Api-Key`)로 간다. Image Server API 키는 이 서버 라우트에서만 쓰고 브라우저에 내보내지 않는다. 응답의 `{ id, url }`을 에디터에 쓴다.
- 업로드된 이미지는 글을 저장할 때 백엔드가 확정(confirm)하고, 저장 안 된 이미지는 Image Server가 정리한다. 프론트가 직접 삭제 로직을 만들지 않는다.
- 업로드 제한 값(형식·크기)은 `constants/imageUpload.ts`에 둔다. 이 라우트는 `formData()`로 파일을 메모리에 올린 뒤에야 크기를 검사한다.
- 업로드가 실패하거나 제한시간을 넘기면 `RichContentEditor`의 `catch`가 자리표시 이미지를 지우고 업로드 중 카운트를 내린다. 업로드 경로를 바꿀 때 이 정리가 계속 실행되는지 확인한다 (안 되면 저장 버튼이 "업로드 중"으로 막힌 채 남는다).

### 기록

- 컴포넌트를 **생성**하면 한 줄 요약을 `Document/front_fn_design/componet_descript.md`에 추가한다: `경로 — 역할 (사용한 패턴)`.
- 의존성은 쓰는 단계에서 설치한다. 새 라이브러리를 들이기 전에 기존 것으로 되는지 먼저 본다.
- 주석: 코드로 알 수 있는 건 쓰지 않는다. **왜 이렇게 했는지**만 적는다.

## 작업 방식

- **화면은 화면으로 검증한다.** 시각적인 변경은 타입 체크·린트로 끝내지 않고 실제 화면(스크린샷)으로 확인한다. 확인하지 못했으면 "화면 확인 못 함"이라고 적는다.
- **검증하고 보고한다.** 린트·타입 체크·빌드·실제 실행 중 한 일과 못 한 일을 구분해서 말한다. 실행해 보지 않은 것을 "동작한다"고 하지 않는다.
- **없는 내용을 지어내지 않는다.** 비어 있는 항목은 채우지 않고 물어본다.
- **범위를 지킨다.** 요청하지 않은 리팩터링·기능 추가·파일 정리를 끼워 넣지 않는다.
- **되돌리기 어려운 작업은 먼저 묻는다.** 삭제, 덮어쓰기, 강제 푸시, 운영 서버에 올리는 작업.
- 결정이 바뀌면 코드와 함께 `Document/`와 이 파일의 "현재 상태"도 갱신한다.

## 보안·비밀

- `.env`, `.env_next`는 열지 않는다.
- 비밀 값은 `.env_next`(git 제외)에만 두고 문서·코드·로그·커밋에 쓰지 않는다.
- 공개 저장소·사이트에 들어가는 문서에는 로컬 경로, 전화번호, 개인 이메일, 내부 주소를 넣지 않는다.

## Windows 환경 주의 (이 PC에서 실제로 겪은 것)

- **dev 서버가 떠 있는 폴더는 `mv`가 파일 잠금으로 실패한다.** 서버를 끄거나 `cp -r` 후 원본 삭제로 우회한다.
- **dev 서버가 떠 있는 파일을 셸 리다이렉트(`cat >`)로 덮어쓰지 않는다.** 순간적으로 빈 파일이 되어 서버가 빈 모듈을 서빙한다. 편집 도구로 고친다.
- Git Bash `curl -F "file=@/c/..."`처럼 POSIX 절대경로는 안 열린다. 그 폴더로 `cd`한 뒤 파일명만 준다.
- 한글 등 비ASCII 페이로드는 명령줄에 직접 넣지 말고 파일로 쓴 뒤 `curl --data-binary @payload.json`으로 보낸다.

## 겪은 버그 — 같은 실수 주의

1. **프론트 토큰 재발급 구조는 폐기됐다**: 예전에는 AT를 메모리(Zustand)에 두고 RT 쿠키로 Silent Refresh를 했다. AS 위임으로 바꾸며 서버 세션 쿠키 방식이 됐다. 401은 재시도하지 않고 로그아웃으로 처리한다.
2. **`NEXT_PUBLIC_*` 변경이 반영되지 않음**: 빌드 때 박히므로 `.env_next`만 고치면 안 된다. 이미지를 다시 빌드한다.
3. **`npm run build`가 `.next/dev/types/routes.d.ts` 문법 오류로 실패**: dev 서버가 쓰다 만 생성 파일이 깨진 채 남아 있었다(2026-09-14 파일). `tsconfig`가 `.next/dev/types`를 포함하므로 빌드 타입 검사까지 막는다. dev 서버를 끈 상태에서 `.next/dev` 폴더를 지우고 다시 빌드한다 (`next dev`가 다시 만든다). 파일 하나만 지우면 `validator.ts`가 그 파일을 찾지 못해 또 실패한다.

## 현재 상태

<마지막 갱신일 2026-11-17>
- 진행: v1.0 운영 중. SEO 메타데이터·sitemap, 어드민 대시보드, MBTI별 투표 통계 화면까지 반영됨. 기능 추가 계속.
- 2026-11-17: 모든 요청에 제한시간 추가(`constants/timeouts.ts` — apiClient, 이미지 업로드, postMetadata, sitemap), 댓글을 `useSuspenseQuery`로 전환, 쓰이지 않던 `usePostDetail`·`useMyProfile` 삭제, 에러 화면 문구를 `getErrorDisplayMessage`로 정리. `npm run build` 통과, axios 시간 초과·연결 거부 코드는 Node 스크립트로 확인. **브라우저 화면 확인은 아직 안 함** (댓글 스켈레톤·에러 시 "다시 시도", 업로드 시간 초과, 에러 문구).
- 2026-11-18: 사용자가 적은 동안 `constants/site.ts`의 `SITEMAP_INCLUDE_POSTS = false`로 sitemap에서 게시글 URL을 뺐다 (`/api/posts/sitemap-entries` 호출 안 함, 정적 경로만). 켜려면 `true`로 바꾸고 다시 빌드한다. 백엔드 엔드포인트는 그대로 남아 있다.
- 알려진 문제·보류 사항:
  - 빌드 시점에 백엔드가 꺼져 있으면 `sitemap.xml`이 정적 경로만으로 만들어지고 `revalidate`(1시간)까지 그대로 서빙된다 (빌드 로그의 `sitemap: error fetching …`).
  - `next-intl`은 설치만 되어 있고 쓰이지 않는다.
  - 자동 테스트 스크립트가 없다.
- 배포: 미니PC에 Docker Compose. 절차는 `Document/build_manual.md`.

이 절은 짧게 유지한다. 단계별 상세 이력은 `Document/`로 옮기고 여기엔 지금 알아야 할 것만 남긴다.
