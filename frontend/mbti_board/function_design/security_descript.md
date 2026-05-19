# Frontend Security — JWT 인증 처리

> 백엔드 설계 기준: `설계/backend_function.md`
> AT TTL: 30분 / RT TTL: 7일 / AT 저장: 메모리 / RT 저장: HttpOnly Cookie

---

## 토큰 저장 전략

| 토큰          | 저장 위치        | 이유                                                    |
| ------------- | ---------------- | ------------------------------------------------------- |
| Access Token  | Zustand (메모리) | XSS 탈취 방어. 새로고침 시 소멸 → Silent Refresh로 복구 |
| Refresh Token | HttpOnly Cookie  | 브라우저가 자동 처리. JS에서 직접 접근 불가             |

```ts
// store/authStore.ts
interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
}
```

---

## Silent Refresh

AT가 만료되었거나 만료 임박 시 RT로 자동 재발급. 사용자는 로그인 상태가 끊기지 않음.

**트리거 시점**

- **401 반응형 갱신**: API에서 401 수신 시 즉시 재발급 시도 후 원래 요청 재시도 (구현이 단순, 권장)

**재발급 실패 시**: RT 만료 또는 Reuse Detection 감지로 서버가 401 반환 → 메모리 AT 초기화 → 로그인 페이지 이동

---

## 탭 간 AT 공유 (BroadcastChannel)

AT는 메모리에 저장되므로 탭마다 AT가 다를 수 있음. `BroadcastChannel`로 탭 간 동기화.

```ts
// lib/authChannel.ts
const channel = new BroadcastChannel("auth");

// AT 재발급 성공 시 다른 탭에 전파
channel.postMessage({ type: "TOKEN_REFRESHED", accessToken });

// 다른 탭에서 수신
channel.onmessage = (e) => {
  if (e.data.type === "TOKEN_REFRESHED") {
    authStore.setAccessToken(e.data.accessToken);
  }
  if (e.data.type === "LOGOUT") {
    authStore.setAccessToken(null);
  }
};
```

**탭 신규 오픈 시**: AT가 없으면 Silent Refresh를 한 번 시도 → 성공 시 AT 획득

---

## API 클라이언트 (axios 인터셉터)

```ts
// lib/apiClient.ts

// 요청 인터셉터: 메모리 AT를 Authorization 헤더에 주입
instance.interceptors.request.use((config) => {
  const token = authStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 응답 인터셉터: 401 수신 시 AT 재발급 후 원래 요청 재시도
instance.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      try {
        const { accessToken } = await refreshAccessToken(); // RT Cookie 자동 전송
        authStore.getState().setAccessToken(accessToken);
        channel.postMessage({ type: "TOKEN_REFRESHED", accessToken });
        error.config.headers.Authorization = `Bearer ${accessToken}`;
        return instance(error.config);
      } catch {
        authStore.getState().setAccessToken(null);
        channel.postMessage({ type: "LOGOUT" });
        redirect("/login");
      }
    }
    return Promise.reject(error);
  },
);
```

---

## Next.js App Router 인증 처리

### Middleware — 라우트 보호

RT(HttpOnly Cookie)는 서버에서 읽을 수 있으므로 Middleware에서 인증 여부 판단.

```ts
// middleware.ts
export function middleware(request: NextRequest) {
  const rt = request.cookies.get("refreshToken");
  const isProtected = request.nextUrl.pathname.startsWith("/mypage");

  if (isProtected && !rt) {
    return NextResponse.redirect(new URL("/login", request.url));
  }
  return NextResponse.next();
}
```

> Middleware는 RT 존재 여부만 확인 (빠른 검증). 실제 유효성 검증은 API 서버에서 수행.

### Server Component

AT에 직접 접근 불가 (메모리 저장이므로). 인증이 필요한 데이터는 Client Component로 위임하거나, 서버에서 RT Cookie로 별도 검증 API 호출.

### Client Component

Zustand의 AT를 사용해 API 요청. 컴포넌트 마운트 시 AT가 없으면 Silent Refresh 시도.

```ts
// 공통 훅
function useRequireAuth() {
  const accessToken = useAuthStore((s) => s.accessToken);
  useEffect(() => {
    if (!accessToken) silentRefresh();
  }, [accessToken]);
}
```

---

## 로그인 / 로그아웃 흐름

**로그인 (Local)**

1. `POST /auth/login` → 응답 body에 AT, Cookie에 RT 자동 저장
2. AT를 Zustand에 저장
3. 다른 탭에 `TOKEN_REFRESHED` 브로드캐스트

**로그인 (소셜 OAuth)**

1. 소셜 로그인 버튼 클릭 → Provider 인증 페이지로 리다이렉트
2. 콜백 URL로 돌아오면 서버가 AT/RT 발급 (Local 로그인과 동일)
3. 이후 처리는 Local 로그인과 동일

**로그아웃**

1. `POST /auth/logout` 요청 (AT 블랙리스트 등록, RT 삭제)
2. Zustand AT 초기화
3. 다른 탭에 `LOGOUT` 브로드캐스트
4. 로그인 페이지로 이동
