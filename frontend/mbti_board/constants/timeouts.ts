// 요청 제한시간. fetch/axios는 기본으로 시간 제한이 없어, 서버가 응답하지 않으면 로딩 화면에서 영원히 멈춘다.
// 서버 라우트(app/api/upload)와 브라우저 양쪽에서 import하므로 "use client"나 서버 전용 코드를 넣지 않는다.

/** 백엔드 API 기본 제한시간 (lib/apiClient.ts) */
export const API_TIMEOUT_MS = 10_000;

/** 제한시간 초과 시 사용자에게 보여 줄 문구 (apiClient의 timeoutErrorMessage, ApiErrorBoundary) */
export const API_TIMEOUT_MESSAGE = "서버 응답이 늦습니다. 잠시 후 다시 시도해 주세요.";

/**
 * 서버 렌더링: 게시글 상세의 generateMetadata → 백엔드 /meta.
 * 크롤러 요청에서는 이 응답을 기다리느라 HTML이 늦어지므로 짧게 둔다. 초과하면 기본 metadata로 렌더링한다.
 */
export const POST_META_TIMEOUT_MS = 3_000;

/** 서버 렌더링: sitemap.xml 생성 시 백엔드 페이지 하나당 제한시간 */
export const SITEMAP_FETCH_TIMEOUT_MS = 10_000;

/** 브라우저 → Next 업로드 라우트. 10MB 업로드와 Image Server 처리 시간을 포함한다. */
export const IMAGE_UPLOAD_TIMEOUT_MS = 30_000;

/**
 * Next 업로드 라우트 → Image Server.
 * 브라우저 쪽(IMAGE_UPLOAD_TIMEOUT_MS)보다 짧게 둬서, 브라우저가 끊기 전에 라우트가 먼저 504를 돌려주게 한다.
 */
export const IMAGE_SERVER_TIMEOUT_MS = 25_000;
