"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { MdMenu, MdAccountCircle } from "react-icons/md";
import { useAuthStore } from "@/store/authStore";
import { useNavStore } from "@/store/navStore";
import { apiClient } from "@/lib/apiClient";
import { ROUTES } from "@/constants/routes";
import { API } from "@/constants/apiEndpoints";
import Logo from "./Logo";

export default function Header() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const setAccessToken = useAuthStore((s) => s.setAccessToken);
  const router = useRouter();
  const openDrawer = useNavStore((s) => s.openDrawer);

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
        <div className="flex-1 flex items-center gap-3 min-w-0">
          <button
            className="md:hidden p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
            onClick={openDrawer}
            aria-label="메뉴 열기"
          >
            <MdMenu size={24} />
          </button>
          <Link href={ROUTES.board.all} className="flex items-center gap-2.5 shrink-0">
            <Logo size="lg" asLink={false}/>
          </Link>
        </div>

        {/* Search */}
        {/* <div className="flex-1 flex justify-center">
          <div className="hidden sm:flex items-center gap-2 bg-surface-container-low px-4 py-2 rounded-full border border-outline-variant w-full max-w-sm">
            <MdSearch className="text-on-surface-variant shrink-0" size={20} />
            <input
              type="text"
              placeholder="게시글 검색..."
              className="bg-transparent border-none outline-none text-sm w-full placeholder:text-on-surface-variant text-foreground"
            />
          </div>
        </div> */}

        {/* Actions */}
        <div className="flex-1 flex items-center justify-end gap-1">
          {accessToken ? (
            <>
              <Link
                href={ROUTES.my}
                className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
                title="마이페이지"
              >
                <MdAccountCircle size={30} />
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
