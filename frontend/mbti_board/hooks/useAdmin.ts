import { useState } from "react";
import { useSuspenseQuery, useSuspenseQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  fetchAdminNotices,
  createAdminNotice,
  updateAdminNotice,
  deleteAdminNotice,
} from "@/lib/api/notice";
import type { NoticeCreateRequest, NoticeUpdateRequest } from "@/types/notice";
import { toast } from "@/components/common/Toast";
import {
  createAdminUser,
  updateAdminUser,
  fetchAdminUsers,
  fetchAdminUserStats,
  fetchAdminPosts,
  fetchAdminPost,
  fetchAdminPostStats,
  changePostVisibility,
  deleteAdminPost,
  fetchAdminComments,
  fetchAdminCommentStats,
  changeCommentVisibility,
  fetchAdminMbtiTestStats,
  fetchAdminMbtiQuestions,
  createAdminMbtiQuestion,
  updateAdminMbtiQuestion,
  changeAdminMbtiQuestionVisibility,
  fetchAdminFunQuestions,
  createAdminFunQuestion,
  updateAdminFunQuestion,
  changeAdminFunQuestionVisibility,
  refreshAdminUserStats,
  refreshAdminPostStats,
  refreshAdminCommentStats,
  refreshAdminMbtiStats,
  fetchAlertStatus,
  muteAlert,
  unmuteAlert,
} from "@/lib/api/admin";
import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  AdminUserListParams,
  AdminPostListParams,
  AdminCommentListParams,
  AdminPostVisibilityRequest,
  AdminCommentVisibilityRequest,
  AlertType,
} from "@/types/admin";
import type { PageResponse } from "@/types/api";
import type { AdminUserSummaryResponse, AdminPostSummaryResponse, AdminCommentSummaryResponse } from "@/types/admin";
import { getApiErrorMessage } from "@/lib/apiError";

/** 이메일 알림 음소거 상태(useAlertStatus) 쿼리 키 — 음소거/해제 mutation 성공 시 invalidate */
export const alertKeys = {
  status: ["admin", "alerts", "status"] as const,
};

/**
 * 어드민 목록/통계 쿼리 키 팩토리 모음
 * 주의: list와 stats는 서로 다른 prefix("list"/"stats")를 가져야 함 —
 *       invalidateQueries는 기본 prefix(부분) 매칭이라 prefix가 겹치면
 *       목록 mutation(삭제·가시성 변경 등)이 무거운 stats 쿼리까지 같이 무효화시켜버림
 */
export const adminKeys = {
  usersList: ["admin", "users", "list"] as const,
  users: (params: AdminUserListParams) => ["admin", "users", "list", params] as const,
  userStats: ["admin", "users", "stats"] as const,

  postsList: ["admin", "posts", "list"] as const,
  posts: (params: AdminPostListParams) => ["admin", "posts", "list", params] as const,
  postDetail: (id: number) => ["admin", "posts", "detail", id] as const,
  postStats: ["admin", "posts", "stats"] as const,

  commentsList: ["admin", "comments", "list"] as const,
  comments: (params: AdminCommentListParams) => ["admin", "comments", "list", params] as const,
  commentStats: ["admin", "comments", "stats"] as const,

  mbtiQuestions: ["admin", "mbti", "questions"] as const,
  mbtiTestStats: ["admin", "mbti", "stats"] as const,
  funQuestions: ["admin", "fun", "questions"] as const,
};

/** 공지사항 목록(useAdminNotices) 쿼리 키 — CRUD mutation 성공 시 invalidate */
export const adminNoticeKeys = {
  all: ["admin", "notices"] as const,
};
/* -------------------------------------------------------------------------------- */

/** AlertType별 이메일 알림 활성화/음소거 상태 조회 */
export function useAlertStatus() {
  return useSuspenseQuery({
    queryKey: alertKeys.status,
    queryFn: fetchAlertStatus,
  });
}

/** 특정 알림 타입 음소거 */
export function useMuteAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (type: AlertType) => muteAlert(type),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.status });
      toast.success("알림이 음소거되었습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "알림 음소거에 실패했습니다."));
    },
  });
}

/** 특정 알림 타입 음소거 해제 */
export function useUnmuteAlert() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (type: AlertType) => unmuteAlert(type),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.status });
      toast.success("알림이 활성화되었습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "알림 활성화에 실패했습니다."));
    },
  });
}



/** 대시보드 요약 카드(총 유저/게시글/댓글 수)용 — 3개 목록 API를 size:1로 병렬 조회 후 totalElements만 추출 */
export function useAdminDashboardStats() {
  const [users, posts, comments] = useSuspenseQueries({
    queries: [
      { queryKey: ["admin", "stats", "users"], queryFn: () => fetchAdminUsers({ size: 1 }), select: (d: PageResponse<AdminUserSummaryResponse>) => d.totalElements },
      { queryKey: ["admin", "stats", "posts"], queryFn: () => fetchAdminPosts({ size: 1 }), select: (d: PageResponse<AdminPostSummaryResponse>) => d.totalElements },
      { queryKey: ["admin", "stats", "comments"], queryFn: () => fetchAdminComments({ size: 1 }), select: (d: PageResponse<AdminCommentSummaryResponse>) => d.totalElements },
    ],
  });
  return {
    totalUsers: users.data,
    totalPosts: posts.data,
    totalComments: comments.data,
  };
}

/** 유저 통계 대시보드(가입 추이·상태·역할 분포 등) 조회 */
export function useAdminUserStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.userStats,
    queryFn: fetchAdminUserStats,
  });
}

/** 게시글 통계 대시보드 조회 */
export function useAdminPostStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.postStats,
    queryFn: fetchAdminPostStats,
  });
}

/** 댓글 통계 대시보드 조회 */
export function useAdminCommentStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.commentStats,
    queryFn: fetchAdminCommentStats,
  });
}

/** MBTI 검사 통계 대시보드 조회 */
export function useAdminMbtiTestStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.mbtiTestStats,
    queryFn: fetchAdminMbtiTestStats,
  });
}


/**
 * 통계 캐시 수동 갱신 훅 팩토리
 * 주의: 갱신 요청은 비동기 배치 작업이라 즉시 반영되지 않음 — 성공 토스트 후 3초 뒤 invalidate로 재조회
 */
function makeStatsRefreshHook<K extends readonly unknown[]>(
  mutationFn: () => Promise<void>,
  queryKey: K,
  label: string,
) {
  return function useRefreshHook() {
    const queryClient = useQueryClient();
    const [isRefreshing, setIsRefreshing] = useState(false);
    const { mutate } = useMutation({
      mutationFn,
      onMutate: () => setIsRefreshing(true),
      onSuccess: () => {
        toast.success(`${label} 캐시 갱신을 시작했습니다. 잠시 후 자동 반영됩니다.`);
        setTimeout(() => {
          queryClient.invalidateQueries({ queryKey });
          setIsRefreshing(false);
        }, 3000);
      },
      onError: (error) => {
        setIsRefreshing(false);
        toast.error(getApiErrorMessage(error, `${label} 캐시 갱신에 실패했습니다.`));
      },
    });
    return { refresh: mutate, isRefreshing };
  };
}

// 통계 유형별 캐시 갱신 훅 (StatsRefreshButton에서 공용 사용)
export const useRefreshAdminUserStats    = makeStatsRefreshHook(refreshAdminUserStats,    adminKeys.userStats,    "사용자 통계");
export const useRefreshAdminPostStats    = makeStatsRefreshHook(refreshAdminPostStats,    adminKeys.postStats,    "게시글 통계");
export const useRefreshAdminCommentStats = makeStatsRefreshHook(refreshAdminCommentStats, adminKeys.commentStats, "댓글 통계");
export const useRefreshAdminMbtiStats    = makeStatsRefreshHook(refreshAdminMbtiStats,    adminKeys.mbtiTestStats,"MBTI 검사 통계");


/** MBTI 문항 전체 목록 조회 (어드민 관리 페이지) */
export function useAdminMbtiQuestions() {
  return useSuspenseQuery({
    queryKey: adminKeys.mbtiQuestions,
    queryFn: fetchAdminMbtiQuestions,
  });
}

/** MBTI 문항 노출 여부 토글 */
export function useChangeAdminMbtiQuestionVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, visible }: { id: number; visible: boolean }) =>
      changeAdminMbtiQuestionVisibility(id, visible),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.mbtiQuestions });
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "노출 상태 변경에 실패했습니다."));
    },
  });
}

/** FUN 문항 전체 목록 조회 */
export function useAdminFunQuestions() {
  return useSuspenseQuery({
    queryKey: adminKeys.funQuestions,
    queryFn: fetchAdminFunQuestions,
  });
}

/** FUN 문항(제목·선택지) 수정 */
/** FUN 문항 생성 */
export function useCreateAdminFunQuestion(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { title: string; options: { optionText: string }[] }) =>
      createAdminFunQuestion(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.funQuestions });
      toast.success("FUN 문항이 추가되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "FUN 문항 추가에 실패했습니다."));
    },
  });
}

export function useUpdateAdminFunQuestion(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: { title: string; options: { id: number; optionText: string }[] } }) =>
      updateAdminFunQuestion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.funQuestions });
      toast.success("FUN 문항이 수정되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "FUN 문항 수정에 실패했습니다."));
    },
  });
}

/** FUN 문항 노출 여부 토글 */
export function useChangeAdminFunQuestionVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, visible }: { id: number; visible: boolean }) =>
      changeAdminFunQuestionVisibility(id, visible),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.funQuestions });
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "노출 상태 변경에 실패했습니다."));
    },
  });
}

/** MBTI 문항(제목·카테고리·역채점 여부) 수정 */
/** MBTI 문항 생성 */
export function useCreateAdminMbtiQuestion(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { title: string; category: string; isReverse: boolean }) =>
      createAdminMbtiQuestion(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.mbtiQuestions });
      toast.success("문항이 추가되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "문항 추가에 실패했습니다."));
    },
  });
}

export function useUpdateAdminMbtiQuestion(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: { title: string; category: string; isReverse: boolean } }) =>
      updateAdminMbtiQuestion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.mbtiQuestions });
      toast.success("문항이 수정되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "문항 수정에 실패했습니다."));
    },
  });
}

/** 유저 목록 조회 — 이메일/상태/역할 필터 + 페이지네이션 */
export function useAdminUsers(params: AdminUserListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.users(params),
    queryFn: () => fetchAdminUsers(params),
  });
}

/** 유저 정보(역할·상태 등) 수정 — 성공 시 params 무관하게 유저 목록 캐시 전체 invalidate */
export function useAdminUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminUpdateUserRequest }) =>
      updateAdminUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.usersList });
      toast.success("유저 정보가 수정되었습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "유저 정보 수정에 실패했습니다."));
    },
  });
}

/** 어드민이 이메일 인증 없이 유저를 직접 생성 */
export function useCreateAdminUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AdminCreateUserRequest) => createAdminUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.usersList });
      toast.success("유저가 생성되었습니다.");
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "유저 생성에 실패했습니다."));
    },
  });
}

/** 게시글 목록 조회 — 유형/가시성/작성자/정렬 필터 + 페이지네이션 */
export function useAdminPosts(params: AdminPostListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.posts(params),
    queryFn: () => fetchAdminPosts(params),
  });
}

/** 게시글 단건 조회(어드민) — 숨겨진 게시글도 조회 가능 */
export function useAdminPost(id: number) {
  return useSuspenseQuery({
    queryKey: adminKeys.postDetail(id),
    queryFn: () => fetchAdminPost(id),
  });
}

/** 게시글 노출 여부(숨기기/복구) 변경 */
export function useChangePostVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AdminPostVisibilityRequest }) =>
      changePostVisibility(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.postsList });
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "게시글 가시성 변경에 실패했습니다."));
    },
  });
}

/** 게시글 삭제 (2단계 확인 후 호출) */
export function useDeleteAdminPost(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAdminPost(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.postsList });
      toast.success("게시글이 삭제되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "게시글 삭제에 실패했습니다."));
    },
  });
}

/** 댓글 목록 조회 — 게시글ID/가시성 필터 + 페이지네이션 */
export function useAdminComments(params: AdminCommentListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.comments(params),
    queryFn: () => fetchAdminComments(params),
  });
}

/** 공지사항 전체 목록 조회 */
export function useAdminNotices() {
  return useSuspenseQuery({
    queryKey: adminNoticeKeys.all,
    queryFn: fetchAdminNotices,
  });
}

/** 공지사항 등록 */
export function useCreateAdminNotice(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: NoticeCreateRequest) => createAdminNotice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminNoticeKeys.all });
      toast.success("공지사항이 등록되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "공지사항 등록에 실패했습니다."));
    },
  });
}

/** 공지사항 수정 */
export function useUpdateAdminNotice(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: NoticeUpdateRequest }) =>
      updateAdminNotice(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminNoticeKeys.all });
      toast.success("공지사항이 수정되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "공지사항 수정에 실패했습니다."));
    },
  });
}

/** 공지사항 삭제 */
export function useDeleteAdminNotice(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAdminNotice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminNoticeKeys.all });
      toast.success("공지사항이 삭제되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "공지사항 삭제에 실패했습니다."));
    },
  });
}

/** 댓글 노출 여부(숨기기/복구) 변경 */
export function useChangeCommentVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AdminCommentVisibilityRequest }) =>
      changeCommentVisibility(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.commentsList });
    },
    onError: (error) => {
      toast.error(getApiErrorMessage(error, "댓글 가시성 변경에 실패했습니다."));
    },
  });
}
