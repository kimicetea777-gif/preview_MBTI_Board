import type { MbtiVerified } from "./user";

export type DebateStance = 1 | -1; // 1:찬성, -1:반대
export type ReactionType = 1 | -1; // 1:좋아요, -1:싫어요

export interface Comment {
  id: number;
  postId: number;
  parentId: number | null;
  authorNickname: string | null;
  authorMbti: string;
  authorMbtiVerified: MbtiVerified;
  content: string;
  likeCount: number;
  dislikeCount: number;
  debateStance: DebateStance | null;
  myReaction: ReactionType | null;
  isVisible: boolean;
  isGuest: boolean;
  isMine: boolean;
  replies: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentRequest {
  content: string;
  parentId?: number | null;
  debateStance?: DebateStance | null;
  guestPassword?: string;
}

export interface ReactionRequest {
  reactionType: ReactionType;
}
