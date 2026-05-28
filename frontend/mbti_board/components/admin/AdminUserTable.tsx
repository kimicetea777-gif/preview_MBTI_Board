"use client";

import { useState } from "react";
import { useAdminUsers, useChangeUserState, useChangeUserRole } from "@/hooks/useAdmin";
import type { UserRole, UserState } from "@/types/user";
import { USER_STATE_LABELS, USER_ROLE_LABELS } from "@/types/admin";
import type { AxiosError } from "axios";

const STATE_OPTIONS: UserState[] = [
  "NORMAL", "PENDING", "INACTIVE", "WITHDRAWN", "LOCKED", "SHADOW_BANNED", "PERMANENT_BAN",
];
const ROLE_OPTIONS: UserRole[] = ["ADMIN", "TESTER", "USER", "GUEST"];
const PAGE_SIZE = 20;

export default function AdminUserTable() {
  const [emailInput, setEmailInput] = useState("");
  const [emailFilter, setEmailFilter] = useState("");
  const [stateFilter, setStateFilter] = useState<UserState | "">("");
  const [roleFilter, setRoleFilter] = useState<UserRole | "">("");
  const [page, setPage] = useState(0);

  const params = {
    email: emailFilter || undefined,
    state: stateFilter || undefined,
    role: roleFilter || undefined,
    page,
    size: PAGE_SIZE,
  };

  const { data, isLoading, isError, error } = useAdminUsers(params);
  const changeState = useChangeUserState();
  const changeRole = useChangeUserRole();

  function handleFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setEmailFilter(emailInput);
    setPage(0);
  }

  const is403 = isError && (error as AxiosError)?.response?.status === 403;

  if (isLoading) return <p className="py-8 text-center text-gray-500">불러오는 중...</p>;
  if (is403) return <p className="py-8 text-center text-red-500">관리자 권한이 없습니다.</p>;
  if (isError) return <p className="py-8 text-center text-red-500">데이터를 불러올 수 없습니다.</p>;

  const users = data?.content ?? [];

  return (
    <section>
      <form onSubmit={handleFilterSubmit} className="mb-4 flex flex-wrap gap-2">
        <input
          type="text"
          placeholder="이메일 검색"
          value={emailInput}
          onChange={(e) => setEmailInput(e.target.value)}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        />
        <select
          value={stateFilter}
          onChange={(e) => { setStateFilter(e.target.value as UserState | ""); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        >
          <option value="">전체 상태</option>
          {STATE_OPTIONS.map((s) => (
            <option key={s} value={s}>{USER_STATE_LABELS[s]}</option>
          ))}
        </select>
        <select
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value as UserRole | ""); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
        >
          <option value="">전체 역할</option>
          {ROLE_OPTIONS.map((r) => (
            <option key={r} value={r}>{USER_ROLE_LABELS[r]}</option>
          ))}
        </select>
        <button
          type="submit"
          className="rounded-md bg-gray-900 px-3 py-1.5 text-sm text-white hover:bg-gray-700"
        >
          검색
        </button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-gray-200">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50 text-xs uppercase text-gray-500">
            <tr>
              <th className="px-4 py-3 text-left">이메일</th>
              <th className="px-4 py-3 text-left">닉네임</th>
              <th className="px-4 py-3 text-left">MBTI</th>
              <th className="px-4 py-3 text-left">역할</th>
              <th className="px-4 py-3 text-left">상태</th>
              <th className="px-4 py-3 text-left">최근 로그인</th>
              <th className="px-4 py-3 text-left">가입일</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 bg-white">
            {users.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-gray-400">
                  유저가 없습니다.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-700">{user.email}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{user.nickname}</td>
                  <td className="px-4 py-3">
                    <span className="rounded bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700">
                      {user.mbtiId}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <select
                      value={user.role}
                      disabled={changeRole.isPending}
                      onChange={(e) =>
                        changeRole.mutate({
                          id: user.id,
                          data: { role: e.target.value as UserRole },
                        })
                      }
                      className="rounded border border-gray-300 px-2 py-1 text-xs focus:border-gray-500 focus:outline-none disabled:opacity-50"
                    >
                      {ROLE_OPTIONS.map((r) => (
                        <option key={r} value={r}>{USER_ROLE_LABELS[r]}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <select
                      value={user.state}
                      disabled={changeState.isPending}
                      onChange={(e) =>
                        changeState.mutate({
                          id: user.id,
                          data: { state: e.target.value as UserState },
                        })
                      }
                      className="rounded border border-gray-300 px-2 py-1 text-xs focus:border-gray-500 focus:outline-none disabled:opacity-50"
                    >
                      {STATE_OPTIONS.map((s) => (
                        <option key={s} value={s}>{USER_STATE_LABELS[s]}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {user.lastLoginAt
                      ? new Date(user.lastLoginAt).toLocaleDateString("ko-KR")
                      : "-"}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {new Date(user.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-sm text-gray-600">
            {page + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={data.last}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </section>
  );
}
