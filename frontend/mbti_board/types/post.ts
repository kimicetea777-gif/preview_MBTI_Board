export const POST_TYPE = {
  FREE: "FREE",
  VOTE: "VOTE",
  DEBATE: "DEBATE",
} as const;

export type PostType = (typeof POST_TYPE)[keyof typeof POST_TYPE];

export const POST_TYPE_LABELS: Record<PostType, string> = {
  FREE: "자유글",
  VOTE: "투표",
  DEBATE: "토론",
};

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

export const VOTE_OPTION_MIN = 2;
export const VOTE_OPTION_MAX = 10;


// GET /api/posts, GET /api/users/me/posts 목록 응답
export interface PostSummaryResponse {
  id: number;
  typeCode: PostType;
  title: string;
  contentPreview: string;          // Tiptap 본문에서 추출한 순수 텍스트 100자
  imageUrls: string[] | null;      // 첫 번째 요소가 썸네일
  authorNickname: string;          // user_id 없으면 백엔드가 "(비회원)"으로 치환 — null 불가
  authorMbtiId: string;
  authorMbtiColor: string;
  mbtiVerified: import("./user").MbtiVerified;
  mbtiVerifiedColor: string;
  targetMbtiId: string | null;     // null = 전체 대상
  viewCount: number;
  commentCount: number;
  createdAt: string;               // ISO 8601
  debateSummary?: DebateResult;              // DEBATE 타입일 때만 non-null
  voteOptions?: Pick<VoteOptionResponse, "id" | "optionText">[]; // VOTE 타입일 때만 non-null, 최대 3개
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
}

// GET /api/posts/{id} 상세 응답
export interface PostDetailResponse extends PostSummaryResponse {
  content: string;                 // Tiptap JSON 직렬화 문자열
  isOwner: boolean;
  isGuest: boolean;
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
