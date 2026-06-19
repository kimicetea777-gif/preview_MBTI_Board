"use client";

import { useState } from "react";
import { FiChevronLeft, FiChevronRight, FiUserPlus } from "react-icons/fi";
import AdminUserDrawer from "./AdminUserDrawer";
import AdminCreateUserModal from "./AdminCreateUserModal";
import { useAdminUsers } from "@/hooks/useAdmin";
import StateInfoPopover from "./StateInfoPopover";
import { USER_ROLE } from "@/types/user";
import type { UserRole, UserState } from "@/types/user";
import { getMbtiColor } from "@/constants/mbtiData";
import {
  USER_STATE_LABELS,
  USER_STATE_COLORS,
  USER_ROLE_LABELS,
  USER_ROLE_COLORS,
  USER_SORT_FIELD,
  USER_SORT_FIELD_LABELS,
  SORT_DIR,
} from "@/types/admin";
import type { UserSortField, SortDir } from "@/types/admin";

const STATE_OPTIONS: UserState[] = [
  "NORMAL", "PENDING", "INACTIVE", "WITHDRAWN", "LOCKED", "SHADOW_BANNED", "PERMANENT_BAN",
];
const ROLE_OPTIONS: UserRole[] = ["ADMIN", "TESTER", "USER", "GUEST"];
const SORT_FIELD_OPTIONS: UserSortField[] = [
  USER_SORT_FIELD.CREATED_AT,
  USER_SORT_FIELD.UPDATED_AT,
  USER_SORT_FIELD.LAST_LOGIN_AT,
];
const PAGE_SIZE = 20;

const INPUT_CLS = "rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

export default function AdminUserTable() {
  const [email, setEmail] = useState("");
  const [state, setState] = useState<UserState | "">("");
  const [role, setRole] = useState<UserRole | "">("");
  const [sortBy, setSortBy] = useState<UserSortField>(USER_SORT_FIELD.CREATED_AT);
  const [sortDir, setSortDir] = useState<SortDir>(SORT_DIR.DESC);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const [appliedParams, setAppliedParams] = useState({
    email: "",
    state: "" as UserState | "",
    role: "" as UserRole | "",
    sortBy: USER_SORT_FIELD.CREATED_AT as UserSortField,
    sortDir: SORT_DIR.DESC as SortDir,
    page: 0,
  });

  const { data } = useAdminUsers({
    email: appliedParams.email || undefined,
    state: appliedParams.state || undefined,
    role: appliedParams.role || undefined,
    sortBy: appliedParams.sortBy,
    sortDir: appliedParams.sortDir,
    page: appliedParams.page,
    size: PAGE_SIZE,
  });

  // 쿼리 데이터에서 파생 — mutation 후 쿼리 무효화 시 자동으로 최신 데이터 반영
  const selectedUser = data.content.find((u) => u.id === selectedUserId) ?? null;


  const INITIAL_FILTERS = {
    email: "",
    state: "" as UserState | "",
    role: "" as UserRole | "",
    sortBy: USER_SORT_FIELD.CREATED_AT as UserSortField,
    sortDir: SORT_DIR.DESC as SortDir,
    page: 0,
  };

  function handleFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedParams({ email, state, role, sortBy, sortDir, page: 0 });
  }

  function handleReset() {
    setEmail(INITIAL_FILTERS.email);
    setState(INITIAL_FILTERS.state);
    setRole(INITIAL_FILTERS.role);
    setSortBy(INITIAL_FILTERS.sortBy);
    setSortDir(INITIAL_FILTERS.sortDir);
    setAppliedParams(INITIAL_FILTERS);
  }

  const users = data.content;

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-sm font-semibold text-on-surface">유저 관리</h2>
        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-1.5 rounded-md bg-primary/70 px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/80"
        >
          <FiUserPlus size={14} />
          유저 추가
        </button>
      </div>

      <form onSubmit={handleFilterSubmit} className="mb-4 flex flex-wrap gap-2">
        <input
          type="text"
          placeholder="이메일 검색"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={INPUT_CLS}
        />
        <select
          value={state}
          onChange={(e) => setState(e.target.value as UserState | "")}
          className={INPUT_CLS}
        >
          <option value="">전체 상태</option>
          {STATE_OPTIONS.map((s) => (
            <option key={s} value={s}>{USER_STATE_LABELS[s]}</option>
          ))}
        </select>
        <select
          value={role}
          onChange={(e) => setRole(e.target.value as UserRole | "")}
          className={INPUT_CLS}
        >
          <option value="">전체 역할</option>
          {ROLE_OPTIONS.map((r) => (
            <option key={r} value={r}>{USER_ROLE_LABELS[r]}</option>
          ))}
        </select>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as UserSortField)}
          className={INPUT_CLS}
        >
          {SORT_FIELD_OPTIONS.map((f) => (
            <option key={f} value={f}>{USER_SORT_FIELD_LABELS[f]} 순</option>
          ))}
        </select>
        <select
          value={sortDir}
          onChange={(e) => setSortDir(e.target.value as SortDir)}
          className={INPUT_CLS}
        >
          <option value={SORT_DIR.DESC}>내림차순</option>
          <option value={SORT_DIR.ASC}>오름차순</option>
        </select>
        <button
          type="submit"
          className="rounded-md bg-primary/70 px-3 py-1.5 text-sm text-primary-foreground hover:bg-primary/80"
        >
          검색
        </button>
        <button
          type="button"
          onClick={handleReset}
          className="rounded-md border border-outline-variant px-3 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low"
        >
          초기화
        </button>
      </form>

      <div className="overflow-x-auto rounded-lg border border-outline-variant">
        <table className="min-w-full text-sm">
          <thead className="bg-surface-container-low text-xs uppercase text-on-surface-variant">
            <tr>
              <th className="px-4 py-3 text-left">이메일</th>
              <th className="px-4 py-3 text-left">닉네임</th>
              <th className="px-4 py-3 text-left">MBTI</th>
              <th className="px-4 py-3 text-left">역할</th>
              <th className="px-4 py-3 text-left">
                <span className="flex items-center gap-1">상태 <StateInfoPopover /></span>
              </th>
              <th className="px-4 py-3 text-left">최근 로그인</th>
              <th className="px-4 py-3 text-left">가입일</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant bg-surface-container-lowest">
            {users.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-8 text-center text-on-surface-variant">
                  유저가 없습니다.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} onClick={() => setSelectedUserId(user.id)} className="cursor-pointer hover:bg-surface-container-low">
                  <td className={`px-4 py-3 font-medium ${user.role === USER_ROLE.ADMIN ? "text-amber-600" : "text-on-surface"}`}>
                    {user.email}
                  </td>
                  <td className="px-4 py-3 font-medium text-on-surface">{user.nickname}</td>
                  <td className="px-4 py-3">
                    <span
                      className="rounded px-2 py-0.5 text-xs font-medium text-white"
                      style={{ backgroundColor: getMbtiColor(user.mbtiId) }}
                    >
                      {user.mbtiId}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className="rounded px-2 py-1 text-xs font-medium text-white whitespace-nowrap"
                      style={{ backgroundColor: USER_ROLE_COLORS[user.role] }}
                    >
                      {USER_ROLE_LABELS[user.role]}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className="rounded px-2 py-1 text-xs font-medium text-white whitespace-nowrap"
                      style={{ backgroundColor: USER_STATE_COLORS[user.state] }}
                    >
                      {USER_STATE_LABELS[user.state]}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    {user.lastLoginAt
                      ? new Date(user.lastLoginAt).toLocaleDateString("ko-KR")
                      : "-"}
                  </td>
                  <td className="px-4 py-3 text-on-surface-variant">
                    {new Date(user.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-outline-variant pt-4">
        <p className="text-sm text-on-surface-variant whitespace-break-spaces">
          총 {data.totalElements}명   :   {Math.min((appliedParams.page + 1) * PAGE_SIZE, data.totalElements)}명 표시
        </p>
        <div className="flex items-center gap-1">
          <button
            onClick={() => setAppliedParams((p) => ({ ...p, page: Math.max(0, p.page - 1) }))}
            disabled={appliedParams.page === 0}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40"
          >
            <FiChevronLeft size={16} />
          </button>

          {(() => {
            const total = data.totalPages;
            const cur = appliedParams.page;
            const pages: (number | "...")[] = [];

            if (total <= 6) {
              for (let i = 0; i < total; i++) pages.push(i);
            } else {
              pages.push(0, 1, 2);
              if (cur > 3) pages.push("...");
              if (cur > 2 && cur < total - 3) pages.push(cur);
              if (cur < total - 4) pages.push("...");
              pages.push(total - 3, total - 2, total - 1);
            }

            return pages.map((p, i) =>
              p === "..." ? (
                <span key={`ellipsis-${i}`} className="px-1 text-xs text-on-surface-variant/50">...</span>
              ) : (
                <button
                  key={p}
                  onClick={() => setAppliedParams((prev) => ({ ...prev, page: p }))}
                  className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-medium transition-colors ${
                    cur === p
                      ? "bg-primary/70 text-primary-foreground shadow-sm"
                      : "border border-outline-variant text-on-surface-variant hover:bg-surface-container-low"
                  }`}
                >
                  {p + 1}
                </button>
              )
            );
          })()}

          <button
            onClick={() => setAppliedParams((p) => ({ ...p, page: Math.min(data.totalPages - 1, p.page + 1) }))}
            disabled={data.last}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-outline-variant text-on-surface-variant hover:bg-surface-container-low disabled:opacity-40"
          >
            <FiChevronRight size={16} />
          </button>
        </div>
      </div>
      <AdminUserDrawer user={selectedUser} onClose={() => setSelectedUserId(null)} />
      {showCreateModal && <AdminCreateUserModal onClose={() => setShowCreateModal(false)} />}
    </section>
  );
}
