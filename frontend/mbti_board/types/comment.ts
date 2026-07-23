import type { MbtiVerified } from "./user";

export type DebateStance = 1 | -1;
export type ReactionType = 1 | -1;

export const STANCE = { PRO: 1, CON: -1 } as const satisfies Record<string, DebateStance>;
export const REACTION = { LIKE: 1, DISLIKE: -1 } as const satisfies Record<string, ReactionType>;

/** 백엔드 CommentResponse와 1:1 매핑 */
export interface Comment {
  id: number;
  content: string;
  authorNickname: string;
  authorMbtiId: string;
  mbtiVerified: MbtiVerified;
  likeCount: number;
  dislikeCount: number;
  myReactionType: ReactionType | null;
  debateStance: DebateStance | null;
  voteOptionText: string | null;
  isGuest: boolean;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
  replies: Comment[];
}

export interface CreateCommentRequest {
  content: string;
  parentId?: number | null;
  debateStance?: DebateStance | null;
  guestPassword?: string;
  termsAgreed?: boolean;
  privacyAgreed?: boolean;
  age14Agreed?: boolean;
}

export interface UpdateCommentRequest {
  content: string;
  guestPassword?: string;
}

export type CommentReactionState = Pick<Comment, "myReactionType" | "likeCount" | "dislikeCount">;

export interface ReactionRequest {
  reactionType: ReactionType;
}

export interface ReactionResponse {
  commentId: number;
  myReactionType: ReactionType | null;
  likeCount: number;
  dislikeCount: number;
}

