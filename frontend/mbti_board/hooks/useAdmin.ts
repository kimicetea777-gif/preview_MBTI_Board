import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  fetchAdminUsers,
  changeUserState,
  changeUserRole,
  fetchAdminPosts,
  changePostVisibility,
  fetchAdminComments,
  changeCommentVisibility,
} from "@/lib/api/admin";
import type {
  AdminUserListParams,
  AdminPostListParams,
  AdminCommentListParams,
  AdminUserStateRequest,
  AdminUserRoleRequest,
  AdminPostVisibilityRequest,
  AdminCommentVisibilityRequest,
} from "@/types/admin";
import type { AxiosError } from "axios";

function getErrorMessage(error: unknown, fallback: string): string {
  const message = (error as AxiosError<{ message?: string | null }>)?.response?.data?.message;
  return message || fallback;
}

export const adminKeys = {
  users: (params: AdminUserListParams) => ["admin", "users", params] as const,
  posts: (params: AdminPostListParams) => ["admin", "posts", params] as const,
  comments: (params: AdminCommentListParams) => ["admin", "comments", params] as const,
};

export function useAdminUsers(params: AdminUserListParams) {
  return useQuery({
    queryKey: adminKeys.users(params),
    queryFn: () => fetchAdminUsers(params),
  });
}

export function useChangeUserState() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminUserStateRequest }) =>
      changeUserState(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("상태가 변경되었습니다.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "상태 변경에 실패했습니다."));
    },
  });
}

export function useChangeUserRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AdminUserRoleRequest }) =>
      changeUserRole(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("역할이 변경되었습니다.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "역할 변경에 실패했습니다."));
    },
  });
}

export function useAdminPosts(params: AdminPostListParams) {
  return useQuery({
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
      toast.error(getErrorMessage(error, "게시글 가시성 변경에 실패했습니다."));
    },
  });
}

export function useAdminComments(params: AdminCommentListParams) {
  return useQuery({
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
      toast.error(getErrorMessage(error, "댓글 가시성 변경에 실패했습니다."));
    },
  });
}
