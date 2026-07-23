export const POST_TYPE_META = {
  FREE:   { label: "자유", color: "bg-primary-container/90 text-surface-variant" },
  VOTE:   { label: "투표",   color: "bg-on-tertiary-container text-surface-variant" },
  DEBATE: { label: "토론",   color: "bg-on-tertiary-fixed-variant text-surface-variant", },
};

export type PostType = keyof typeof POST_TYPE_META;

export const POST_TYPE = {
  FREE: "FREE",
  VOTE: "VOTE",
  DEBATE: "DEBATE",
} as const satisfies Record<PostType, PostType>;

export const POST_TYPE_LABELS: Record<PostType, string> = Object.fromEntries(
  (Object.keys(POST_TYPE_META) as PostType[]).map((k) => [k, POST_TYPE_META[k].label])
) as Record<PostType, string>;

// sort 값은 백엔드 PostSortType enum과 동일하게 대문자 사용
export const POST_SORT = {
  LATEST: "LATEST",
  VIEWS: "VIEWS",
  COMMENTS: "COMMENTS",
} as const;

export type PostSortType = (typeof POST_SORT)[keyof typeof POST_SORT];

export const SORT_OPTIONS: { label: string; value: PostSortType }[] = [
  { label: "최신순", value: POST_SORT.LATEST },
  { label: "인기순", value: POST_SORT.VIEWS },
  { label: "댓글순", value: POST_SORT.COMMENTS },
];

export const VIEW_MODE = {
  CARD: "card",
  LIST: "list",
} as const;

export type ViewMode = (typeof VIEW_MODE)[keyof typeof VIEW_MODE];

export const MAX_TITLE_LENGTH = 150;

export const VOTE_OPTION_MIN = 2;
export const VOTE_OPTION_MAX = 16;
export const MAX_POST_IMAGES = 3;

export const GUEST_PASSWORD_MIN_LENGTH = 4;
export const GUEST_PASSWORD_MAX_LENGTH = 72;


// GET /api/posts, GET /api/users/me/posts 목록 응답
export interface PostSummaryResponse {
  id: number;
  typeCode: PostType;
  title: string;
  contentPreview: string;          // Tiptap 본문에서 추출한 순수 텍스트 100자
  imageUrls: string[] | null;      // 첫 번째 요소가 썸네일
  authorNickname: string;          // user_id 없으면 백엔드가 "(비회원)"으로 치환 — null 불가
  authorMbtiId: string;
  mbtiVerified: import("./user").MbtiVerified;
  targetMbtiId: string | null;     // null = 전체 대상
  viewCount: number;
  commentCount: number;
  createdAt: string;               // ISO 8601
  debateSummary?: DebateResult;              // DEBATE 타입일 때만 non-null
  voteOptions?: Pick<VoteOptionResponse, "id" | "optionText" | "voteCount">[]; // VOTE 타입일 때만 non-null, 최대 3개
}

// GET /api/users/me/posts/preview 응답 (최근 5개)
export interface MyPostPreview {
  id: number;
  typeCode: PostType;
  title: string;
  commentCount: number;
  viewCount: number;
  createdAt: string;
}

// GET /api/users/me/posts 쿼리 파라미터
export interface MyPostListParams {
  type?: PostType;       // 생략 시 전체 유형 조회
  sort?: PostSortType;
  page?: number;
  size?: number;
}

// 투표 선택지 응답
export interface VoteOptionResponse {
  id: number;
  optionText: string;
  mbtiTargetId: string | null;
  voteCount: number;
}

// 투표 결과 응답 (GET /api/posts/{id}/vote/result, POST /api/posts/{id}/vote 공통)
export interface VoteResult {
  options: VoteOptionResponse[];
  myVotedOptionId: number | null;
  isEnded: boolean;
  endsAt: string | null;           // ISO 8601, null이면 무기한
}

// 토론 결과 응답 (GET /api/posts/{id} 포함)
export interface DebateResult {
  proLabel: string;
  conLabel: string;
  proCount: number;
  conCount: number;
  isClosed: boolean;
  endsAt: string | null;           // ISO 8601, null이면 무기한
  myStance: 1 | -1 | null;        // 현재 사용자의 진영 투표 (비로그인·미투표 시 null)
}

// GET /api/posts/{id}/meta 응답 — SEO/OG 미리보기 생성 전용, viewCount 증가 없음
export interface PostMetaResponse {
  title: string;
  contentPreview: string;
  imageUrls: string[] | null;
}

// GET /api/posts/sitemap-entries 응답 — sitemap.xml 생성 전용, JOIN 없는 최소 프로젝션
export interface PostSitemapEntry {
  id: number;
  createdAt: string; // ISO 8601
}

// GET /api/posts/{id} 상세 응답
export interface PostDetailResponse extends PostSummaryResponse {
  content: string;                 // Tiptap JSON 직렬화 문자열
  isOwner: boolean;
  isGuest: boolean;
  isVisible: boolean;
  updatedAt: string;
  voteResult?: VoteResult;         // VOTE 타입일 때만 포함
  debateResult?: DebateResult;     // DEBATE 타입일 때만 포함
}

export interface PostListParams {
  type?: PostType;
  sort?: PostSortType;
  authorMbti?: string;
  targetMbti?: string;             // 생략 또는 "ALL" = 전체
  page?: number;
  size?: number;
}

// 투표 선택지 요청
export interface VoteOptionRequest {
  optionText: string;
  mbtiTarget?: string;             // MBTI 투표 모드 전용 — 일반 모드는 undefined
}

// POST /api/posts 요청 (FREE 전용 — VOTE·DEBATE는 전용 엔드포인트 사용)
export interface PostCreateRequest {
  type: "FREE";
  title: string;
  content: string;                 // Tiptap JSON 직렬화 문자열
  guestPassword?: string;
  targetMbti?: string;
  termsAgreed?: boolean;
  privacyAgreed?: boolean;
  age14Agreed?: boolean;
}

// POST /api/posts/vote 요청 (회원 전용)
export interface VotePostCreateRequest {
  title: string;
  content: string;                 // Tiptap JSON 직렬화 문자열
  targetMbti?: string;
  voteOptions: VoteOptionRequest[];
  endsAt?: string;                 // ISO 8601
}

// POST /api/posts/debate 요청 (회원 전용)
export interface DebatePostCreateRequest {
  title: string;
  content: string;                 // Tiptap JSON 직렬화 문자열
  targetMbti?: string;
  endsAt?: string;                 // ISO 8601, null이면 무기한
  proLabel?: string;               // null이면 기본값 "찬성"
  conLabel?: string;               // null이면 기본값 "반대"
}

// PATCH /api/posts/{id} 요청
export interface PostUpdateRequest {
  title: string;
  content: string;
  guestPassword?: string;
}
