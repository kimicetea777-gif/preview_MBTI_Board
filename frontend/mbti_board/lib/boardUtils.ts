import { ROUTES } from "@/lib/routes";
import { POST_TYPE } from "@/types/post";
import type { PostType } from "@/types/post";

export const BOARD_META: Record<PostType | "ALL", { title: string; description: string; writeHref: string }> = {
  ALL:    { title: "전체 게시글",  description: "모든 게시판의 최신 글을 확인하세요.",     writeHref: ROUTES.board.freeWrite },
  FREE:   { title: "자유 게시판",  description: "자유롭게 생각을 나눠보세요.",              writeHref: ROUTES.board.freeWrite },
  VOTE:   { title: "투표 게시판",  description: "투표로 의견을 모아보세요.",                writeHref: ROUTES.board.voteWrite },
  DEBATE: { title: "토론 게시판",  description: "찬반 토론으로 깊은 대화를 나눠보세요.",   writeHref: ROUTES.board.debateWrite },
};

export function postHref(typeCode: PostType, id: number): string {
  if (typeCode === POST_TYPE.VOTE) return ROUTES.board.votePost(id);
  if (typeCode === POST_TYPE.DEBATE) return ROUTES.board.debatePost(id);
  return ROUTES.board.freePost(id);
}
