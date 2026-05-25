import { useQuery } from "@tanstack/react-query";
import { fetchComments } from "@/lib/api/comment";

export const commentKeys = {
  all: ["comments"] as const,
  byPost: (postId: number) => ["comments", postId] as const,
};

export function useComments(postId: number) {
  return useQuery({
    queryKey: commentKeys.byPost(postId),
    queryFn: () => fetchComments(postId),
  });
}
