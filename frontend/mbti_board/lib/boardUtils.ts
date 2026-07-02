import { ROUTES } from "@/constants/routes";
import { POST_TYPE } from "@/types/post";
import type { PostType } from "@/types/post";

export function postHref(typeCode: PostType, id: number): string {
  if (typeCode === POST_TYPE.VOTE) return ROUTES.board.votePost(id);
  if (typeCode === POST_TYPE.DEBATE) return ROUTES.board.debatePost(id);
  return ROUTES.board.freePost(id);
}
