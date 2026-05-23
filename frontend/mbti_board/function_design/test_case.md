# Frontend 테스트 계획

## 테스트 환경 설정

### 필요 패키지

```bash
npm install -D jest jest-environment-jsdom @testing-library/react @testing-library/user-event @testing-library/jest-dom msw
```

> `ts-jest` 대신 Next.js 내장 `next/jest` transformer 사용.
> `@/` alias, `next/font`, `next/image` 등 Next.js 의존성을 별도 설정 없이 처리함.

### 설정 파일

| 파일                  | 역할                                                          |
| --------------------- | ------------------------------------------------------------- |
| `jest.config.ts`      | `createJestConfig(nextJest({ dir: './' }))` — 환경: jsdom     |
| `jest.setup.ts`       | `@testing-library/jest-dom` 확장 매처 임포트                  |
| `__mocks__/server.ts` | MSW `setupServer` 핸들러 등록 (API 모킹, P3 훅 테스트용)      |

---

## 테스트 우선순위

```
P1 (즉시) — 순수 함수: 설정 없이 작성 가능, 리그레션 위험 높음
P2 (Phase 완료 후) — 컴포넌트: jsdom 환경 필요
P3 (안정화 후) — 훅 / 통합: MSW + QueryClient 래퍼 필요
```

---

## P1 — 순수 함수 단위 테스트

### `lib/safeRedirect.ts` — `toSafeRedirect`

**파일**: `__tests__/lib/safeRedirect.test.ts`

| 입력                      | 기대값                 | 설명                   |
| ------------------------- | ---------------------- | ---------------------- |
| `"/board/free"`           | `"/board/free"`        | 정상 내부 경로         |
| `"/"`                     | `"/"`                  | 루트 경로              |
| `"/board/free?page=2"`    | `"/board/free?page=2"` | 쿼리스트링 포함        |
| `"//evil.com"`            | `"/"`                  | 프로토콜 상대 URL 차단 |
| `"http://evil.com"`       | `"/"`                  | 절대 외부 URL 차단     |
| `"https://evil.com/path"` | `"/"`                  | HTTPS 외부 URL 차단    |
| `""`                      | `"/"`                  | 빈 문자열 폴백         |
| `"javascript:alert(1)"`   | `"/"`                  | XSS 시도 차단          |

---

### `components/post/PostCard.tsx` — `formatRelativeTime` (내부 함수)

> `formatRelativeTime`은 현재 `PostCard.tsx` 내부에 선언되어 있음.
> 테스트를 위해 `lib/formatRelativeTime.ts`로 분리 후 export 권장.

**파일**: `__tests__/lib/formatRelativeTime.test.ts`

| 조건                     | 기대값                                          |
| ------------------------ | ----------------------------------------------- |
| 현재 시각 기준 30초 전   | `"방금 전"`                                     |
| 45분 전                  | `"45분 전"`                                     |
| 3시간 전                 | `"3시간 전"`                                    |
| 10일 전                  | `"10일 전"`                                     |
| 31일 전                  | `"YYYY. M. D."` 형식 (toLocaleDateString ko-KR) |
| 경계값: 정확히 60분 전   | `"1시간 전"` (`minutes < 60` 직후 분기)         |
| 경계값: 정확히 24시간 전 | `"1일 전"` (`hours < 24` 직후 분기)             |
| 경계값: 정확히 30일 전   | `"YYYY. M. D."` 형식 (`days < 30` 조건 미충족)  |

---

### `app/(main)/board/free/[id]/PostDetailContent.tsx` — `isTiptapEmpty`

> `isTiptapEmpty`는 `write/page.tsx`에도 동일하게 정의되어 있음.
> `lib/isTiptapEmpty.ts`로 분리 후 두 파일에서 import 권장.

**파일**: `__tests__/lib/isTiptapEmpty.test.ts`

| 입력                       | 기대값  | 설명                          |
| -------------------------- | ------- | ----------------------------- |
| `""`                       | `true`  | 빈 문자열                     |
| `undefined / null` (falsy) | `true`  | 값 없음                       |
| `"invalid json {"`         | `true`  | JSON 파싱 실패                |
| 텍스트 없는 빈 doc JSON    | `true`  | `{"type":"doc","content":[]}` |
| 공백만 있는 paragraph      | `true`  | `" "` 텍스트 노드             |
| 일반 텍스트 포함           | `false` | `"안녕하세요"` 텍스트 노드    |
| 중첩 구조 (h2 > text)      | `false` | 헤딩 안에 텍스트              |
| 텍스트 없는 bulletList     | `true`  | 리스트 아이템 내용 없음       |
| 코드 블록만 있는 경우      | `false` | code 노드 안에 텍스트         |

---

### `hooks/usePosts.ts` — `postKeys`

**파일**: `__tests__/hooks/postKeys.test.ts`

| 메서드                                                        | 기대값 (deep equality)                                          |
| ------------------------------------------------------------- | --------------------------------------------------------------- |
| `postKeys.all()`                                              | `["posts"]`                                                     |
| `postKeys.lists()`                                            | `["posts", "list"]`                                             |
| `postKeys.list({ type: "FREE" })`                             | `["posts", "list", { type: "FREE" }]`                           |
| `postKeys.list({ type: "FREE", sort: "LATEST" })`             | `["posts", "list", { type: "FREE", sort: "LATEST" }]`           |
| `postKeys.details()`                                          | `["posts", "detail"]`                                           |
| `postKeys.detail(42)`                                         | `["posts", "detail", 42]`                                       |
| `postKeys.list({ type:"FREE" })` vs `postKeys.list({type:"FREE", sort:"VIEWS"})` | `toEqual` 결과 다름 (TQ는 deep equality로 캐시 구분) |
| `postKeys.detail(1)` vs `postKeys.detail(2)`                  | `toEqual` 결과 다름                                             |

> 참조 비교(`!==`)는 매 호출마다 새 배열을 반환하므로 항상 `true` — `toEqual`을 사용할 것.

---

## P1 (추가) — `middleware.ts` 라우트 보호

> Next.js `middleware`는 Node.js 환경에서 직접 단위 테스트 어려움.
> `NextRequest` mock + `NextResponse` spy로 검증하거나 E2E(Playwright)로 커버.

**파일**: `__tests__/middleware.test.ts`

| 경로 | session 쿠키 | 기대 동작 |
|------|-------------|-----------|
| `/my` | 없음 | `/login?redirect=%2Fmy` 로 리다이렉트 |
| `/my/settings` | 없음 | `/login?redirect=%2Fmy%2Fsettings` 로 리다이렉트 |
| `/board/vote/write` | 없음 | `/login?redirect=...` 로 리다이렉트 |
| `/board/debate/write` | 없음 | `/login?redirect=...` 로 리다이렉트 |
| `/board/free/write` | 없음 | 통과 (비회원 작성 허용, PROTECTED 목록 미포함) |
| `/my` | 있음 | 통과 (`NextResponse.next()`) |
| `/board/vote/write` | 있음 | 통과 |
| `/board/free/123` | 없음 | 통과 (일반 게시글 상세, 보호 대상 아님) |

---

## P2 — 컴포넌트 테스트

### `components/post/GuestPasswordModal.tsx`

**파일**: `__tests__/components/GuestPasswordModal.test.tsx`

#### 렌더링

| 조건                         | 기대 동작                     |
| ---------------------------- | ----------------------------- |
| `open=false`                 | 컴포넌트 미렌더링 (null 반환) |
| `open=true, action="update"` | "비회원 수정 확인" 헤더 표시  |
| `open=true, action="delete"` | "비회원 삭제 확인" 헤더 표시  |
| `error="잘못된 비밀번호"`    | 에러 메시지 표시              |
| `isPending=true`             | input, 버튼 disabled 상태     |

#### 상호작용

| 시나리오                        | 기대 동작                             |
| ------------------------------- | ------------------------------------- |
| 3자 입력 후 확인 버튼 클릭               | `onConfirm` 미호출 (버튼 disabled)           |
| 4자 이상 입력 후 확인 버튼 클릭          | `onConfirm(password)` 호출                   |
| `isPending=true` + 4자 입력 후 확인 클릭 | `onConfirm` 미호출 (`disabled={isPending \|\| !isPasswordValid}`) |
| 폼 submit (Enter)                        | `onConfirm(password)` 호출                   |
| 취소 버튼 클릭                           | `onClose` 호출                               |
| 배경(overlay) 클릭                       | `onClose` 호출                               |
| 모달 내부 클릭                           | `onClose` 미호출 (이벤트 버블링 차단)        |
| `open` false → true 전환                 | password 입력값 초기화                       |

---

### `components/common/MbtiBadge.tsx`

**파일**: `__tests__/components/MbtiBadge.test.tsx`

> 컴포넌트는 아이콘 없이 텍스트와 배경 색상만 사용.
> `verified="DEFAULT"`일 때 `mbti` 값 대신 `"미인증"` 텍스트가 표시됨.
> `mbti` prop 타입은 `string` (non-nullable) — null/undefined는 TypeScript 레벨에서 차단.

| 입력                                 | 기대 동작                                              |
| ------------------------------------ | ------------------------------------------------------ |
| `mbti="INFP", verified="TESTED"`     | `"INFP"` 텍스트 표시, `title="검사인증"`               |
| `mbti="ESTJ", verified="SELF"`       | `"ESTJ"` 텍스트 표시, `title="자기입력"`               |
| `mbti="INFJ", verified="DEFAULT"`    | `"미인증"` 텍스트 표시 (mbti 값 무시), `title="미인증"` |
| verified 불일치 (존재하지 않는 값)   | 런타임 에러 없음, VERIFIED_COLOR 키 누락 시 undefined  |

---

### `components/post/PostCard.tsx`

**파일**: `__tests__/components/PostCard.test.tsx`

| 시나리오                        | 기대 동작                      |
| ------------------------------- | ------------------------------ |
| `targetMbtiId=null`             | "에게" 배지 미표시             |
| `targetMbtiId="INFP"`           | "INFP에게" 배지 표시           |
| 클릭                            | `/board/free/{id}` 링크로 이동 |
| `commentCount=5, viewCount=100` | "댓글 5", "조회 100" 표시      |

---

## P3 — 훅 / 통합 테스트

> MSW + `renderHook` + `QueryClient` wrapper 필요.

### `hooks/usePosts.ts` — `usePostDetail` initialData 로직

**파일**: `__tests__/hooks/usePostDetail.test.ts`

| 시나리오                  | 기대 동작                                |
| ------------------------- | ---------------------------------------- |
| 목록 캐시 없을 때         | `initialData` undefined → 로딩 상태      |
| 목록 캐시에 id=42 존재    | `initialData`로 즉시 summary 데이터 반환 |
| `initialDataUpdatedAt: 0` | 캐시 있어도 백그라운드 fetch 즉시 실행   |
| `enabled: id > 0` 검증    | `id=0` 이면 fetch 미실행                 |

### `hooks/usePosts.ts` — `usePostList` 무한 스크롤

| 시나리오             | 기대 동작                                     |
| -------------------- | --------------------------------------------- |
| 첫 페이지 fetch      | `pages[0].content` 반환                       |
| `last=false`         | `hasNextPage=true`, `fetchNextPage` 호출 가능 |
| `last=true`          | `hasNextPage=false`                           |
| `fetchNextPage` 호출 | `pageParam = number + 1`로 다음 요청          |

---

## 리팩터링 선행 작업 (테스트 전 권장)

아래 두 함수는 현재 여러 파일에 중복 정의되어 있어 테스트하려면 분리가 필요합니다.

| 현재 위치               | 중복 위치        | 분리 대상                   |
| ----------------------- | ---------------- | --------------------------- |
| `PostDetailContent.tsx` | `write/page.tsx` | `lib/isTiptapEmpty.ts`      |
| `PostCard.tsx` (내부)   | —                | `lib/formatRelativeTime.ts` |

분리 후 기존 파일에서 import로 교체하면 됩니다.
