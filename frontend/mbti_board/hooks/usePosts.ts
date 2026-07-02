import { useEffect } from "react";
import { useSuspenseInfiniteQuery, useQuery, useSuspenseQuery, useQueryClient } from "@tanstack/react-query";
import type { InfiniteData, QueryClient } from "@tanstack/react-query";
import { fetchPosts, fetchPostById } from "@/lib/api/post";
import type { PostListParams, PostSummaryResponse, PostDetailResponse } from "@/types/post";
import type { PageResponse } from "@/types/api";
import { recordRecentlyViewed } from "@/lib/recentlyViewedPosts";

export const postKeys = {
  all:     () => ["posts"]                    as const,
  lists:   () => [...postKeys.all(), "list"]  as const,
  list:    (params: Omit<PostListParams, "page">) =>
               [...postKeys.lists(), params]  as const,
  details: () => [...postKeys.all(), "detail"] as const,
  detail:  (id: number) =>
               [...postKeys.details(), id]    as const,
};

/**
 * 목록 캐시(무한 스크롤, 정렬·필터별 여러 개 존재 가능)에서 id가 일치하는 게시글을 찾아
 * 상세 쿼리의 initialData로 사용 — summary 필드만 있어 content/isOwner/isGuest/updatedAt은 없음 (UI에서 nullish 처리 필요)
 */
function findPostInListCaches(
  queryClient: QueryClient,
  id: number,
): PostDetailResponse | undefined {
  const listCaches = queryClient.getQueriesData<
    InfiniteData<PageResponse<PostSummaryResponse>>
  >({ queryKey: postKeys.lists() });

  for (const [, cached] of listCaches) {
    for (const page of cached?.pages ?? []) {
      const match = page.content.find((post) => post.id === id);
      if (match) return match as unknown as PostDetailResponse;
    }
  }
  return undefined;
}

export function useSuspensePostList(params: Omit<PostListParams, "page">) {
  return useSuspenseInfiniteQuery({
    queryKey: postKeys.list(params),
    queryFn: ({ pageParam }) => fetchPosts({ ...params, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.last ? undefined : lastPage.number + 1,
  });
}

export function useSuspensePostDetail(id: number) {
  const queryClient = useQueryClient();

  const result = useSuspenseQuery({
    queryKey: postKeys.detail(id),
    queryFn: () => fetchPostById(id),
    initialData: () => findPostInListCaches(queryClient, id),
    initialDataUpdatedAt: 0,
  });

  const { id: postId, typeCode, title } = result.data;
  useEffect(() => {
    if (!typeCode) return;
    recordRecentlyViewed({ id: postId, typeCode, title });
  }, [postId, typeCode, title]);

  return result;
}

export function usePostDetail(id: number) {
  const queryClient = useQueryClient();

  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: () => fetchPostById(id),
    enabled: id > 0,
    initialData: () => findPostInListCaches(queryClient, id),
    // 0으로 설정 시 initialData를 즉시 stale로 취급 → 백그라운드에서 전체 상세 데이터 fetch
    initialDataUpdatedAt: 0,
  });
}
