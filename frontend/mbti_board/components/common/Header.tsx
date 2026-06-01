"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { MdSearch, MdNotificationsNone, MdAccountCircle } from "react-icons/md";
import { useAuthStore } from "@/store/authStore";
import { apiClient } from "@/lib/apiClient";
import { ROUTES } from "@/lib/routes";
import { API } from "@/lib/apiEndpoints";

export default function Header() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const setAccessToken = useAuthStore((s) => s.setAccessToken);
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await apiClient.post(API.auth.logout);
    } finally {
      setAccessToken(null);
      router.push(ROUTES.login);
    }
  };

  return (
    <header className="sticky top-0 z-50 border-b border-outline-variant bg-background">
      <div className="mx-auto flex h-16 max-w-300 items-center justify-between px-8 gap-4">

        {/* Brand */}
        <div className="flex-1 flex items-center min-w-0">
          <Link href={ROUTES.board.all} className="flex items-center gap-2.5 shrink-0">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden>
              <circle cx="14" cy="14" r="14" fill="#1b3022" />
              <path d="M9 20c0-5 3-8 5-9 2 1 5 4 5 9" stroke="#c4ef48" strokeWidth="1.5" strokeLinecap="round" />
              <line x1="14" y1="11" x2="14" y2="20" stroke="#c4ef48" strokeWidth="1.5" strokeLinecap="round" />
              <path d="M11 14.5c1-1.5 2-2 3-2" stroke="#c4ef48" strokeWidth="1.2" strokeLinecap="round" />
            </svg>
            <span className="font-jakarta font-bold text-lg text-primary tracking-tight">
              MBTI Board
            </span>
          </Link>
        </div>

        {/* Search */}
        <div className="flex-1 flex justify-center">
          <div className="hidden sm:flex items-center gap-2 bg-surface-container-low px-4 py-2 rounded-full border border-outline-variant w-full max-w-sm">
            <MdSearch className="text-on-surface-variant shrink-0" size={20} />
            <input
              type="text"
              placeholder="게시글 검색..."
              className="bg-transparent border-none outline-none text-sm w-full placeholder:text-on-surface-variant text-foreground"
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex-1 flex items-center justify-end gap-1">
          <button className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors">
            <MdNotificationsNone size={22} />
          </button>

          {accessToken ? (
            <>
              <Link
                href={ROUTES.my}
                className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
                title="마이페이지"
              >
                <MdAccountCircle size={22} />
              </Link>
              <button
                onClick={handleLogout}
                className="ml-1 px-4 py-1.5 rounded-full text-sm font-medium text-on-surface-variant border border-outline-variant hover:bg-accent transition-colors"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link
              href={ROUTES.login}
              className="ml-1 px-4 py-1.5 rounded-full text-sm font-semibold bg-primary text-primary-foreground hover:opacity-90 transition-opacity"
            >
              로그인
            </Link>
          )}
        </div>

      </div>
    </header>
  );
}
