import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/common/Toast";
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
      toast.success("?곹깭媛 蹂寃쎈릺?덉뒿?덈떎.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "?곹깭 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
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
      toast.success("??븷??蹂寃쎈릺?덉뒿?덈떎.");
    },
    onError: (error) => {
      toast.error(getErrorMessage(error, "??븷 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
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
      toast.error(getErrorMessage(error, "寃뚯떆湲 媛?쒖꽦 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
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
      toast.error(getErrorMessage(error, "?볤? 媛?쒖꽦 蹂寃쎌뿉 ?ㅽ뙣?덉뒿?덈떎."));
    },
  });
}

