"use client";

import { useState } from "react";
import { FiX, FiTrash2, FiRotateCcw, FiCopy, FiCheck, FiExternalLink } from "react-icons/fi";
import { useAdminUpdateUser } from "@/hooks/useAdmin";
import { AUTH_SERVER_URL } from "@/constants/site";
import { getMbtiColor } from "@/constants/mbtiData";
import { USER_ROLE, MBTI_VERIFIED_META } from "@/types/user";
import type { UserRole, UserState } from "@/types/user";
import {
  USER_STATE_LABELS,
  USER_ROLE_LABELS,
} from "@/types/admin";
import StateInfoPopover from "./StateInfoPopover";
import type { AdminUserSummaryResponse } from "@/types/admin";

const STATE_OPTIONS: UserState[] = [
  "NORMAL", "PENDING", "INACTIVE", "WITHDRAWN", "LOCKED", "SHADOW_BANNED", "PERMANENT_BAN",
  "MEMBERSHIP_WITHDRAWAL_PENDING",
];
const ROLE_OPTIONS: UserRole[] = ["ADMIN", "TESTER", "USER", "GUEST"];

interface Props {
  user: AdminUserSummaryResponse | null;
  onClose: () => void;
}

export default function AdminUserDrawer({ user, onClose }: Props) {
  if (!user) return null;
  // key로 유저가 바뀔 때마다 아래 컴포넌트를 새로 마운트해 draft 상태를 초기화
  return <AdminUserDrawerContent key={user.id} user={user} onClose={onClose} />;
}

function AdminUserDrawerContent({ user, onClose }: { user: AdminUserSummaryResponse; onClose: () => void }) {
  const [draftNickname, setDraftNickname] = useState(user.nickname);
  const [draftRole, setDraftRole] = useState<UserRole>(user.role);
  const [draftState, setDraftState] = useState<UserState>(user.state);
  const [clearProfileImage, setClearProfileImage] = useState(false);
  const [idCopied, setIdCopied] = useState(false);

  const updateUser = useAdminUpdateUser();

  async function handleCopyId() {
    try {
      await navigator.clipboard.writeText(user.authServerId);
      setIdCopied(true);
      setTimeout(() => setIdCopied(false), 1500);
    } catch { }
  }

  const nicknameChanged = draftNickname.trim() !== user.nickname;
  const roleChanged = draftRole !== user.role;
  const stateChanged = draftState !== user.state;
  const hasChanges = nicknameChanged || roleChanged || stateChanged || clearProfileImage;

  const isPending = updateUser.isPending;

  function handleSave() {
    if (!hasChanges || isPending) return;
    updateUser.mutate({
      id: user.id,
      data: {
        nickname: nicknameChanged ? draftNickname.trim() : undefined,
        clearProfileImage: clearProfileImage || undefined,
        role: roleChanged ? draftRole : undefined,
        state: stateChanged ? draftState : undefined,
      },
    });
  }

  const verifiedMeta = MBTI_VERIFIED_META[user.mbtiVerified];

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose} />
      <aside className="fixed right-0 top-0 z-50 flex h-full w-96 flex-col border-l border-outline-variant bg-surface-container-lowest shadow-xl">
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <h2 className="text-sm font-semibold text-on-surface">유저 상세</h2>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-md text-on-surface-variant hover:bg-surface-container-low"
          >
            <FiX size={16} />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
          {/* 프로필 헤더 */}
          <div className="flex items-center gap-3">
            <div className="h-14 w-14 shrink-0">
              {user.profileImageUrl && !clearProfileImage ? (
                <img
                  src={user.profileImageUrl}
                  alt={user.nickname}
                  className="h-full w-full rounded-full object-cover border border-outline-variant"
                />
              ) : (
                <div className="h-full w-full rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant text-lg font-semibold">
                  {user.nickname[0]?.toUpperCase()}
                </div>
              )}
            </div>
            <div className="min-w-0">
              <p className={`truncate text-sm font-semibold ${user.role === USER_ROLE.ADMIN ? "text-amber-600" : "text-on-surface"}`}>
                {user.email}
              </p>
              <div className="mt-1 flex flex-wrap items-center gap-1.5">
                <span
                  className="rounded px-1.5 py-0.5 text-xs font-medium text-white"
                  style={{ backgroundColor: getMbtiColor(user.mbtiId) }}
                >
                  {user.mbtiId || "미설정"}
                </span>
                <span
                  className="rounded px-1.5 py-0.5 text-xs font-medium text-white"
                  style={{ backgroundColor: verifiedMeta.color }}
                >
                  {verifiedMeta.label}
                </span>
              </div>
            </div>
          </div>

          {/* 읽기 전용 정보 */}
          <dl className="rounded-lg border border-outline-variant divide-y divide-outline-variant text-xs">
            <div className="flex items-center justify-between gap-2 px-3 py-2">
              <dt className="text-on-surface-variant shrink-0">AS 유저 ID</dt>
              <dd className="flex min-w-0 items-center gap-1.5">
                <span className="truncate font-mono text-[11px] text-on-surface" title={user.authServerId}>{user.authServerId}</span>
                <button
                  onClick={handleCopyId}
                  title="복사"
                  className="shrink-0 text-on-surface-variant hover:text-on-surface"
                >
                  {idCopied ? <FiCheck size={12} /> : <FiCopy size={12} />}
                </button>
              </dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-on-surface-variant">로그인 방식</dt>
              <dd className="font-medium text-on-surface">{user.isSocialLogin ? "소셜 로그인" : "로컬 로그인"}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-on-surface-variant">가입일</dt>
              <dd className="font-medium text-on-surface">{new Date(user.createdAt).toLocaleDateString("ko-KR")}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-on-surface-variant">최근 수정일</dt>
              <dd className="font-medium text-on-surface">{new Date(user.updatedAt).toLocaleDateString("ko-KR")}</dd>
            </div>
            <div className="flex justify-between px-3 py-2">
              <dt className="text-on-surface-variant">최근 로그인</dt>
              <dd className="font-medium text-on-surface">
                {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString("ko-KR") : "-"}
              </dd>
            </div>
          </dl>

          <a
            href={`${AUTH_SERVER_URL}/admin/membership?userId=${user.authServerId}`}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center gap-1.5 rounded-md border border-outline-variant px-3 py-1.5 text-xs text-on-surface-variant hover:bg-surface-container-low"
          >
            <FiExternalLink size={13} />
            AS 어드민에서 조회
          </a>

          <hr className="border-outline-variant" />

          {/* 닉네임 */}
          <div className="flex items-center gap-3">
            <label className="w-20 shrink-0 text-xs font-medium text-on-surface-variant">닉네임</label>
            <input
              type="text"
              value={draftNickname}
              onChange={(e) => setDraftNickname(e.target.value)}
              maxLength={12}
              className="flex-1 rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none"
            />
          </div>

          {/* 프로필 사진 */}
          <div className="flex items-center gap-3">
            <label className="w-20 shrink-0 text-xs font-medium text-on-surface-variant">프로필 사진</label>
            {user.profileImageUrl ? (
              clearProfileImage ? (
                <button
                  onClick={() => setClearProfileImage(false)}
                  className="flex items-center gap-1.5 rounded-md border border-outline-variant px-3 py-1.5 text-xs text-on-surface-variant hover:bg-surface-container-low"
                >
                  <FiRotateCcw size={13} />
                  삭제 취소
                </button>
              ) : (
                <button
                  onClick={() => setClearProfileImage(true)}
                  className="flex items-center gap-1.5 rounded-md border border-outline-variant px-3 py-1.5 text-xs text-on-surface-variant hover:bg-surface-container-low"
                >
                  <FiTrash2 size={13} />
                  부적절한 사진 삭제
                </button>
              )
            ) : (
              <p className="text-xs text-on-surface-variant/60">프로필 사진 없음</p>
            )}
          </div>

          {/* 역할 */}
          <div className="flex items-center gap-3">
            <label className="w-20 shrink-0 text-xs font-medium text-on-surface-variant">역할</label>
            <select
              value={draftRole}
              onChange={(e) => setDraftRole(e.target.value as UserRole)}
              className="flex-1 rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none"
            >
              {ROLE_OPTIONS.map((r) => (
                <option key={r} value={r}>{USER_ROLE_LABELS[r]}</option>
              ))}
            </select>
          </div>

          {/* 상태 */}
          <div className="flex items-center gap-3">
            <label className="w-20 shrink-0 flex items-center gap-1 text-xs font-medium text-on-surface-variant">상태 <StateInfoPopover /></label>
            <select
              value={draftState}
              onChange={(e) => setDraftState(e.target.value as UserState)}
              className="flex-1 rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none"
            >
              {STATE_OPTIONS.map((s) => (
                <option key={s} value={s}>{USER_STATE_LABELS[s]}</option>
              ))}
            </select>
          </div>
        </div>

        <footer className="border-t border-outline-variant px-5 py-4">
          <button
            onClick={handleSave}
            disabled={!hasChanges || isPending}
            className="w-full rounded-md bg-primary py-2 text-sm font-medium text-primary-foreground hover:bg-primary/80 disabled:opacity-40"
          >
            수정
          </button>
        </footer>
      </aside>
    </>
  );
}
