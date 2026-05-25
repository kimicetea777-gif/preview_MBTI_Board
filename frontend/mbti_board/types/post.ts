export type PostType = "FREE" | "VOTE" | "DEBATE";

// sort 값은 백엔드 PostSortType enum과 동일하게 대문자 사용
export const POST_SORT = {
  LATEST: "LATEST",
  VIEWS: "VIEWS",
  COMMENTS: "COMMENTS",
} as const;

export type PostSortType = (typeof POST_SORT)[keyof typeof POST_SORT];

export const VOTE_OPTION_MIN = 2;
export const VOTE_OPTION_MAX = 10;

// GET /api/posts 목록 응답
export interface PostSummaryResponse {
  id: number;
  title: string;
  authorNickname: string;          // user_id 없으면 백엔드가 "(비회원)"으로 치환 — null 불가
  authorMbtiId: string;
  authorMbtiColor: string;
  mbtiVerified: import("./user").MbtiVerified;
  mbtiVerifiedColor: string;
  targetMbtiId: string | null;     // null = 전체 대상
  viewCount: number;
  commentCount: number;
  createdAt: string;               // ISO 8601
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

// GET /api/posts/{id} 상세 응답
export interface PostDetailResponse extends PostSummaryResponse {
  content: string;                 // Tiptap JSON 직렬화 문자열
  isOwner: boolean;
  isGuest: boolean;
  updatedAt: string;
  voteResult?: VoteResult;         // VOTE 타입일 때만 포함
}


export interface PostListParams {
  type: PostType;
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

// POST /api/posts 요청
export interface PostCreateRequest {
  type: PostType;
  title: string;
  content: string;                 // Tiptap JSON 직렬화 문자열
  guestPassword?: string;
  targetMbti?: string;
  voteOptions?: VoteOptionRequest[]; // VOTE 타입 전용
  endsAt?: string;                   // ISO 8601, VOTE·DEBATE 공통 마감일
}

// PATCH /api/posts/{id} 요청
export interface PostUpdateRequest {
  title: string;
  content: string;
  guestPassword?: string;
}
