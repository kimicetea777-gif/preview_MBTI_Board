export type PostType = "FREE" | "VOTE" | "DEBATE";
export type SortType = "LATEST" | "COMMENT" | "VIEW";

export interface Post {
  id: number;
  authorNickname: string | null;
  authorMbti: string;
  authorMbtiVerified: import("./user").MbtiVerified;
  typeCode: PostType;
  title: string;
  content: object;
  viewCount: number;
  commentCount: number;
  isVisible: boolean;
  isGuest: boolean;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostRequest {
  typeCode: PostType;
  title: string;
  content: object;
  guestPassword?: string;
  // VOTE 전용
  voteOptions?: VoteOptionRequest[];
  endsAt?: string | null;
  // DEBATE 전용
  proLabel?: string;  // ex) 찬성, 나는 싫소
  conLabel?: string;  // ex) 반대, 나느 좋소
}

export interface VoteOptionRequest {
  optionText: string;   // 자유 선택지 값
  mbtiTarget?: string | null; //MBTI 선택지 값
  displayOrder: number;
}

export interface VoteOption {
  id: number;
  optionText: string;
  mbtiTarget: string | null;
  displayOrder: number;
  voteCount: number;
}

export interface VoteResult {
  options: VoteOption[];
  myOptionId: number | null;
  totalVoteCount: number;
  isClosed: boolean;
}

export interface DebateSummary {
  proLabel: string;
  conLabel: string;
  proCount: number;
  conCount: number;
  isClosed: boolean;
  endsAt: string | null;
}
