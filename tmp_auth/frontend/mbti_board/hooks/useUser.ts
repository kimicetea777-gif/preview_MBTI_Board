import { useQuery, useSuspenseQuery } from "@tanstack/react-query";
import { fetchMyProfile, fetchMyActivityStats, fetchMyPostsPreview, fetchMyMbtiHistory } from "@/lib/api/user";

export const userKeys = {
  all:             () => ["user"]                               as const,
  myProfile:       () => [...userKeys.all(), "myProfile"]       as const,
  myActivityStats: () => [...userKeys.all(), "activityStats"]   as const,
  myPostsPreview:  () => [...userKeys.all(), "myPostsPreview"]  as const,
  myMbtiHistory:   () => [...userKeys.all(), "myMbtiHistory"]   as const,
};

export function useMyProfile() {
  return useQuery({
    queryKey: userKeys.myProfile(),
    queryFn: fetchMyProfile,
  });
}

export function useMyProfileSuspense() {
  return useSuspenseQuery({
    queryKey: userKeys.myProfile(),
    queryFn: fetchMyProfile,
  });
}

export function useMyActivityStatsSuspense() {
  return useSuspenseQuery({
    queryKey: userKeys.myActivityStats(),
    queryFn: fetchMyActivityStats,
  });
}

export function useMyPostsPreviewSuspense() {
  return useSuspenseQuery({
    queryKey: userKeys.myPostsPreview(),
    queryFn: fetchMyPostsPreview,
  });
}

export function useMyMbtiHistorySuspense() {
  return useSuspenseQuery({
    queryKey: userKeys.myMbtiHistory(),
    queryFn: fetchMyMbtiHistory,
  });
}
