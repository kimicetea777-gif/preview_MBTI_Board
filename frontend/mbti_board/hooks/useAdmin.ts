import { useSuspenseQuery, useSuspenseQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/common/Toast";
import {
  createAdminUser,
  updateAdminUser,
  fetchAdminUsers,
  fetchAdminUserStats,
  fetchAdminPosts,
  fetchAdminPostStats,
  changePostVisibility,
  deleteAdminPost,
  fetchAdminComments,
  fetchAdminCommentStats,
  changeCommentVisibility,
  fetchAdminMbtiTestStats,
  fetchAdminMbtiQuestions,
  updateAdminMbtiQuestion,
  changeAdminMbtiQuestionVisibility,
  fetchAdminFunQuestions,
  updateAdminFunQuestion,
  changeAdminFunQuestionVisibility,
} from "@/lib/api/admin";
import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  AdminUserListParams,
  AdminPostListParams,
  AdminCommentListParams,
  AdminPostVisibilityRequest,
  AdminCommentVisibilityRequest,
} from "@/types/admin";
import type { PageResponse } from "@/types/api";
import type { AdminUserSummaryResponse, AdminPostSummaryResponse, AdminCommentSummaryResponse } from "@/types/admin";
import type { AxiosError } from "axios";

function getErrorMessage(error: unknown, fallback: string): string {
  const message = (error as AxiosError<{ message?: string | null }>)?.response?.data?.message;
  return message || fallback;
}

export const adminKeys = {
  users: (params: AdminUserListParams) => ["admin", "users", params] as const,
  userStats: ["admin", "users", "stats"] as const,
  posts: (params: AdminPostListParams) => ["admin", "posts", params] as const,
  postStats: ["admin", "posts", "stats"] as const,
  comments: (params: AdminCommentListParams) => ["admin", "comments", params] as const,
  commentStats: ["admin", "comments", "stats"] as const,
  mbtiQuestions: ["admin", "mbti", "questions"] as const,
  mbtiTestStats: ["admin", "mbti", "stats"] as const,
  funQuestions: ["admin", "fun", "questions"] as const,
};

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

export function useAdminUserStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.userStats,
    queryFn: fetchAdminUserStats,
  });
}

export function useAdminPostStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.postStats,
    queryFn: fetchAdminPostStats,
  });
}

export function useAdminCommentStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.commentStats,
    queryFn: fetchAdminCommentStats,
  });
}

export function useAdminMbtiTestStats() {
  return useSuspenseQuery({
    queryKey: adminKeys.mbtiTestStats,
    queryFn: fetchAdminMbtiTestStats,
  });
}

export function useAdminMbtiQuestions() {
  return useSuspenseQuery({
    queryKey: adminKeys.mbtiQuestions,
    queryFn: fetchAdminMbtiQuestions,
  });
}

export function useChangeAdminMbtiQuestionVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, visible }: { id: number; visible: boolean }) =>
      changeAdminMbtiQuestionVisibility(id, visible),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.mbtiQuestions });
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "노출 상태 변경에 실패했습니다."));
    },
  });
}

export function useAdminFunQuestions() {
  return useSuspenseQuery({
    queryKey: adminKeys.funQuestions,
    queryFn: fetchAdminFunQuestions,
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
      toast.error(getErrorMessage(error, "FUN 문항 수정에 실패했습니다."));
    },
  });
}

export function useChangeAdminFunQuestionVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, visible }: { id: number; visible: boolean }) =>
      changeAdminFunQuestionVisibility(id, visible),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.funQuestions });
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "노출 상태 변경에 실패했습니다."));
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
      toast.error(getErrorMessage(error, "문항 수정에 실패했습니다."));
    },
  });
}

export function useAdminUsers(params: AdminUserListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.users(params),
    queryFn: () => fetchAdminUsers(params),
  });
}

export function useAdminUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminUpdateUserRequest }) =>
      updateAdminUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("유저 정보가 수정되었습니다.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "유저 정보 수정에 실패했습니다."));
    },
  });
}

export function useCreateAdminUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: AdminCreateUserRequest) => createAdminUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("유저가 생성되었습니다.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "유저 생성에 실패했습니다."));
    },
  });
}

export function useAdminPosts(params: AdminPostListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.posts(params),
    queryFn: () => fetchAdminPosts(params),
  });
}

export function useChangePostVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AdminPostVisibilityRequest }) =>
      changePostVisibility(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "posts"] });
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "寃뚯떆湲 媛?쒖꽦 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
    },
  });
}

export function useDeleteAdminPost(onSuccess?: () => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAdminPost(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "posts"] });
      toast.success("게시글이 삭제되었습니다.");
      onSuccess?.();
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "게시글 삭제에 실패했습니다."));
    },
  });
}

export function useAdminComments(params: AdminCommentListParams) {
  return useSuspenseQuery({
    queryKey: adminKeys.comments(params),
    queryFn: () => fetchAdminComments(params),
  });
}

export function useChangeCommentVisibility() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: AdminCommentVisibilityRequest }) =>
      changeCommentVisibility(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "comments"] });
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "?볤? 媛?쒖꽦 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
    },
  });
}
