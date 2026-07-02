# 다음 개발 단계

`dev_steps.md`(완료된 로드맵) 이후 진행할 작업. Phase 단위 수직 슬라이스, 백엔드 → 프론트 순서.

---

## Phase — 소셜 로그인 약관 동의 게이트

> 현재 문제: (1) OAuth2 신규 가입 시 약관 동의 없이 `CustomOAuth2UserService.createUser()`가 즉시 `users` row를 생성하고 토큰까지 발급함. (2) 이메일 가입도 프론트 체크박스(`FormStep.tsx`)만 강제할 뿐 동의 여부가 DB 어디에도 저장되지 않음 — 두 경로 모두 법적 동의 증빙이 없음.
>
> 목표: **동의를 받기 전에는 `users` 테이블에 아무 row도 생성하지 않는다.** 구글 인증은 신원 확인 용도로만 쓰고, 프로필 정보는 DB가 아니라 짧은 만료시간의 서명 토큰에 담아 프론트로 전달 → 동의 완료 시점에 비로소 계정을 생성.
>
> 범위: 카카오는 아직 연동 전이라 이번 Phase에서 제외 (구글만 대상). 카카오 연동 시 동일 구조(`OAuth2UserInfo` 인터페이스 공유)를 그대로 적용하되, 이메일 선택 동의 이슈는 그때 별도 검토.

### [Backend]

#### 1. DB — 동의 시각 + 버전 컬럼 추가

- 신규 Flyway 마이그레이션 (`V15__add_user_terms_agreement.sql`)
  ```sql
  ALTER TABLE users ADD COLUMN terms_agreed_at TIMESTAMPTZ;
  ALTER TABLE users ADD COLUMN terms_version INT;
  ALTER TABLE users ADD COLUMN privacy_agreed_at TIMESTAMPTZ;
  ALTER TABLE users ADD COLUMN privacy_version INT;
  ```
- 버전 관리 필요(결정됨) — 약관/개인정보처리방침 내용이 바뀌면 "몇 번째 버전에 동의했는지"를 구분해야 함
  - 현재 버전을 상수로 관리 (예: `TermsPolicy.CURRENT_TERMS_VERSION`, `TermsPolicy.CURRENT_PRIVACY_VERSION`)
  - 동의 시점에 이 상수값을 `terms_version`/`privacy_version`에 그대로 저장
  - 향후 버전이 올라가면(`CURRENT_TERMS_VERSION` 증가) 로그인 시 `user.termsVersion < CURRENT_TERMS_VERSION`인 유저를 감지해 재동의를 요구하는 흐름을 추가할 수 있음 (이번 Phase 범위 밖, 컬럼만 미리 준비)
- `User` 엔티티에 `termsAgreedAt`/`termsVersion`/`privacyAgreedAt`/`privacyVersion` 필드 + `agreeToTerms(Instant now, int termsVersion, int privacyVersion)` 메소드 추가
- **레거시 유저(기존 가입자)는 이 컬럼들이 영구히 NULL로 남음 — 소급 동의 안 받기로 결정, 별도 마이그레이션/백필 불필요**

#### 2. OAuth2 신규 가입 — DB 저장 없이 임시 서명 토큰 발급

- `CustomOAuth2UserService.loadUser()` 분기 변경
  - 기존 회원(`provider`+`providerId` 매칭) → 지금과 동일하게 `validateAccountState()` 후 정상 로그인
  - 신규 회원 → **`userRepository.save()` 호출하지 않음.** 이메일 null/blank 체크, `existsByEmail` 중복 체크는 기존처럼 여기서 먼저 수행(가입 불가능한 경우 동의 화면까지 보내지 않기 위함) 후, `User` 없이 프로필 정보(`provider`, `providerId`, `email`, `nickname`, `profileImageUrl`)만 담은 별도 principal(예: `PendingOAuth2SignupPrincipal`) 반환
- `OAuth2AuthenticationSuccessHandler` 분기 변경
  - `PendingOAuth2SignupPrincipal`인 경우: `tokenIssuerService.issue()` 호출하지 않고, 프로필 정보를 **10분 만료(확정)**의 서명 토큰(`signupToken`)으로 발급 → `/terms/agree#signupToken=...`로 리다이렉트
  - 기존 회원인 경우: 지금과 동일하게 AT 발급 + `/oauth2/callback#accessToken=...` 리다이렉트
- `signupToken` 발급/검증 로직 — `JwtProvider`와 동일한 서명 키 재사용 여부는 구현 시 결정 (신규 메소드 추가 vs 별도 컴포넌트)
- **`createUser()` 위치 재구성 필요** — 현재 `CustomOAuth2UserService`의 private 메소드라 계정 생성이 이 클래스 안에서만 일어남. 검증(이메일 null/중복 체크)은 `loadUser()`에 남기고, 실제 `User` 생성·저장 로직은 3번(동의 완료 API)이 호출할 수 있는 곳으로 옮기거나 공유 서비스로 추출해야 함
- **감사 로그 이동** — 기존엔 `loadUser()`에서 `isNew` 여부로 `AuditEvent.OAUTH2_SIGNUP`/`OAUTH2_LOGIN`을 기록했는데, 신규 유저는 이제 `loadUser()` 시점엔 `User`가 아직 없어서(`user.getId()` 호출 불가) `OAUTH2_SIGNUP` 기록은 3번(동의 완료 API)으로 옮겨야 함. `OAUTH2_LOGIN`(기존 회원)은 지금처럼 `loadUser()`에 남음
- **SecurityConfig 화이트리스트 추가 필요** — `SecurityConfig.java`의 `permitAll` 목록(`/api/auth/signup`, `/api/auth/login` 등)에 신규 엔드포인트 `POST /api/auth/oauth2/signup`을 추가하지 않으면 401 발생
- **확인 완료(조치 불필요)**: `/terms/agree`는 `middleware.ts`의 matcher(`/my/:path*`, `/admin/:path*`, `/board/write`)에 안 걸려서 세션 쿠키 없이도 접근 가능함을 확인함

#### 3. 동의 완료 API — 이 시점에 처음으로 계정 생성

- `POST /api/auth/oauth2/signup` — `{ signupToken, termsAgreed, privacyAgreed }`
  - `signupToken` 서명·만료 검증 (`ErrorCode`에 관련 코드 추가 필요, 예: `SIGNUP_TOKEN_EXPIRED`, `TERMS_NOT_AGREED`)
  - `termsAgreed`/`privacyAgreed` 둘 다 `true`가 아니면 400
  - **멱등성 처리**: 같은 `signupToken`으로 중복 호출될 가능성 있음 — `provider`+`providerId`로 이미 생성된 계정이 있으면 새로 만들지 않고 그 계정으로 로그인 처리. **이 분기에서는 `agreeToTerms()`를 호출하지 않음** — `termsAgreedAt`/`privacyAgreedAt`/버전은 최초 생성 시점에만 기록하고, 이미 존재하는 계정의 기존 값을 덮어쓰면 안 됨(최초 동의 시각이 실제 동의 시각과 달라지는 걸 방지)
  - **동시성(TOCTOU) 처리**: `loadUser()`에서 이메일 중복을 체크한 시점과 실제 계정 생성(여기) 사이 최대 10분의 간극이 있음 — 그 사이 같은 이메일로 다른 경로(이메일 가입 등)가 먼저 가입하면 `users.email UNIQUE` 제약 위반이 발생할 수 있으므로, INSERT 시점에 `existsByEmail` 재검증 또는 `DataIntegrityViolationException`을 잡아 `DUPLICATE_EMAIL`로 매핑 필요 (닉네임도 동일 종류의 경합 가능성 있으나 발생 확률 낮음)
  - 신규 생성 시 `state: NORMAL`(현재 `PENDING` 안 거침), `termsAgreedAt`/`privacyAgreedAt = now()`
  - `TokenIssuerService.issue()`로 AT/RT 정상 발급 후 `LoginResponse` 반환

#### 4. 이메일 가입 경로도 동의 시각 기록 (이번 Phase 범위 포함 — 확정)

- `SignupRequest`(FE `types/auth.ts` + BE DTO)에 `termsAgreed`/`privacyAgreed` 필드 추가 (프론트에서 이미 체크 강제 중이므로 값은 항상 `true`로 오지만, 백엔드도 방어적으로 `false`면 400 반환)
- `AuthService.signup()`에서 `agreeToTerms(Instant.now(), TermsPolicy.CURRENT_TERMS_VERSION, TermsPolicy.CURRENT_PRIVACY_VERSION)` 호출

### [Frontend]

#### 5. OAuth2 콜백 분기

- `OAuth2CallbackContent.tsx` — URL hash에 `accessToken` 대신 `signupToken`이 오는 경우를 분기 처리
  - `accessToken` 있음 → 기존 로직 그대로 (AT 저장 후 원래 목적지로 리다이렉트)
  - `signupToken` 있음 → `/terms/agree#signupToken=...`로 이동 (accessToken과 동일하게 fragment 사용 — 서버 로그에 안 남고 Referer에도 안 실림). **`OAUTH2_REDIRECT_KEY`/`PENDING_SOCIAL_PROVIDER_KEY`(sessionStorage)를 이 분기에서 지우면 안 됨** — 지금 accessToken 분기는 이 두 키를 읽고 바로 삭제하는데, signupToken 분기는 그대로 남겨둬서 동의 완료 후 `TermsAgreementContent.tsx`가 읽어야 함

#### 6. 약관 동의 페이지 신설 (소셜 전용 이름 지양 — 향후 재동의 흐름과 공유)

- `app/(auth)/terms/agree/page.tsx` + `TermsAgreementContent.tsx`
  - 이름/라우트를 "소셜 가입"이 아니라 "약관 동의"로 범용화 — 나중에 약관 버전이 올라가서 **기존 유저(이메일/소셜 무관) 재동의**가 필요해질 때도 같은 화면을 재사용하기 위함
  - 이번 Phase에서는 URL의 `signupToken` 유무로만 분기: 있으면 신규 소셜 가입 완료 흐름(`POST /api/auth/oauth2/signup`), 없는 경우(기존 유저 재동의)는 이번 범위 밖이라 미구현
- `FormStep.tsx`(148-181행)에 있는 약관/개인정보 체크박스 UI를 공용 컴포넌트로 추출(`TermsAgreementCheckboxes.tsx`) — `WriteFormContent.tsx`(비회원 자유글 작성 시 동일 체크박스 존재)까지 포함해 3곳 중복 제거
- 동의 클릭 → `POST /api/auth/oauth2/signup` 호출 → 성공 시:
  1. AT 저장
  2. `sessionStorage`에서 `PENDING_SOCIAL_PROVIDER_KEY` 읽어 `setLastLoginMethod(provider)` 호출 (지금 `OAuth2CallbackContent.tsx`가 하던 역할을 이 페이지가 이어받음)
  3. `OAUTH2_REDIRECT_KEY`로 원래 목적지 리다이렉트
  - 실패(토큰 만료 등) 시 재로그인 유도

---

## 결정 사항 (확정)

1. 이메일 가입 경로도 이번 Phase에 포함 (4번 — 배포 전 상태라 굳이 나눌 필요 없음)
2. 레거시 유저는 `terms_agreed_at`/`privacy_agreed_at` NULL로 그대로 둠 (배포 전이라 소급 동의 불필요)
3. `signupToken` 만료 10분 확정
4. 약관 버전 관리 도입 — `terms_version`/`privacy_version` 컬럼 추가, 현재 버전은 상수(`TermsPolicy`)로 관리
5. 카카오 연동 미완료 상태라 이번 Phase는 구글만 대상으로 진행 — 카카오 관련 항목(이메일 선택 동의 이슈 등)은 제외
