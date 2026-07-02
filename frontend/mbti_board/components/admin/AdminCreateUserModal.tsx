"use client";

import { useState } from "react";
import { FiX } from "react-icons/fi";
import { useCreateAdminUser } from "@/hooks/useAdmin";
import { USER_ROLE_LABELS, USER_STATE_LABELS } from "@/types/admin";
import { MBTI_ID } from "@/types/mbti";
import type { UserRole, UserState } from "@/types/user";

const ROLE_OPTIONS: UserRole[] = ["ADMIN", "TESTER", "USER", "GUEST"];
// PENDING(이메일 인증 대기)은 어드민 직접 생성 시 의미 없음 — 이메일 인증 우회
// WITHDRAWN(탈퇴 대기)은 생성 시 허용 불가 — 백엔드에서도 400 반환
const STATE_OPTIONS: UserState[] = ["NORMAL", "INACTIVE", "LOCKED", "SHADOW_BANNED", "PERMANENT_BAN"];
const MBTI_OPTIONS = Object.values(MBTI_ID);

const INPUT_CLS =
  "w-full rounded-md border border-outline-variant bg-surface-container-lowest px-3 py-1.5 text-sm text-on-surface focus:border-outline focus:outline-none";

interface Props {
  onClose: () => void;
}

export default function AdminCreateUserModal({ onClose }: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [mbtiId, setMbtiId] = useState<string>(MBTI_ID.NONE);
  const [role, setRole] = useState<UserRole>("USER");
  const [state, setState] = useState<UserState>("NORMAL");

  const createUser = useCreateAdminUser();

  const passwordMismatch = passwordConfirm.length > 0 && password !== passwordConfirm;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (createUser.isPending || password !== passwordConfirm) return;
    createUser.mutate(
      { email, password, nickname, mbtiId, role, state },
      { onSuccess: onClose }
    );
  }

  return (
    <>
      <div className="fixed inset-0 z-40 bg-black/30" onClick={onClose} />
      <div className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-xl border border-outline-variant bg-surface-container-lowest shadow-xl">
        <header className="flex items-center justify-between border-b border-outline-variant px-5 py-4">
          <h2 className="text-sm font-semibold text-on-surface">유저 직접 생성</h2>
          <button
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-md text-on-surface-variant hover:bg-surface-container-low"
          >
            <FiX size={16} />
          </button>
        </header>

        <form onSubmit={handleSubmit} className="space-y-3 px-5 py-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className={INPUT_CLS}
              placeholder="user@example.com"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">
              비밀번호 <span className="text-on-surface-variant/60">(영문+숫자 포함 10자 이상)</span>
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={10}
              className={INPUT_CLS}
              placeholder="임시 비밀번호"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">비밀번호 확인</label>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              required
              className={`${INPUT_CLS} ${passwordMismatch ? "border-destructive" : ""}`}
              placeholder="비밀번호 재입력"
            />
            {passwordMismatch && (
              <p className="mt-1 text-xs text-destructive">비밀번호가 일치하지 않습니다.</p>
            )}
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">
              닉네임 <span className="text-on-surface-variant/60">(2~12자, 한글·영문·숫자·_)</span>
            </label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              required
              minLength={2}
              maxLength={12}
              className={INPUT_CLS}
              placeholder="닉네임"
            />
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="mb-1 block text-xs font-medium text-on-surface-variant">MBTI</label>
              <select
                value={mbtiId}
                onChange={(e) => setMbtiId(e.target.value)}
                className={INPUT_CLS}
              >
                {MBTI_OPTIONS.map((id) => (
                  <option key={id} value={id}>{id}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-on-surface-variant">역할</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
                className={INPUT_CLS}
              >
                {ROLE_OPTIONS.map((r) => (
                  <option key={r} value={r}>{USER_ROLE_LABELS[r]}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-medium text-on-surface-variant">상태</label>
              <select
                value={state}
                onChange={(e) => setState(e.target.value as UserState)}
                className={INPUT_CLS}
              >
                {STATE_OPTIONS.map((s) => (
                  <option key={s} value={s}>{USER_STATE_LABELS[s]}</option>
                ))}
              </select>
            </div>
          </div>

          <footer className="flex justify-end gap-2 border-t border-outline-variant pt-4">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-outline-variant px-4 py-1.5 text-sm text-on-surface-variant hover:bg-surface-container-low"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={createUser.isPending || password !== passwordConfirm}
              className="rounded-md bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:bg-primary/80 disabled:opacity-40"
            >
              {createUser.isPending ? "생성 중..." : "생성"}
            </button>
          </footer>
        </form>
      </div>
    </>
  );
}
