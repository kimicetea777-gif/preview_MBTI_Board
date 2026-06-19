import { useEffect } from "react";
import { useSuspenseInfiniteQuery, useQuery, useSuspenseQuery, useQueryClient } from "@tanstack/react-query";
import type { InfiniteData } from "@tanstack/react-query";
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
    initialData: () => {
      const listCaches = queryClient.getQueriesData<
        InfiniteData<PageResponse<PostSummaryResponse>>
      >({ queryKey: postKeys.lists() });

      for (const [, cached] of listCaches) {
        for (const page of cached?.pages ?? []) {
          const match = page.content.find((post) => post.id === id);
          if (match) return match as unknown as PostDetailResponse;
        }
      }
    },
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
    initialData: () => {
      // 목록 캐시에서 해당 게시글의 summary 데이터를 찾아 초기값으로 사용.
      // content/isOwner/isGuest/updatedAt은 아직 없으므로 UI에서 nullish 처리 필요.
      const listCaches = queryClient.getQueriesData<
        InfiniteData<PageResponse<PostSummaryResponse>>
      >({ queryKey: postKeys.lists() });

      for (const [, cached] of listCaches) {
        for (const page of cached?.pages ?? []) {
          const match = page.content.find((post) => post.id === id);
          if (match) return match as unknown as PostDetailResponse;
        }
      }
    },
    // 0으로 설정 시 initialData를 즉시 stale로 취급 → 백그라운드에서 전체 상세 데이터 fetch
    initialDataUpdatedAt: 0,
  });
}
