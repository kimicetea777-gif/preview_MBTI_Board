"use client";

import Link from "next/link";
import { MdMenu, MdAccountCircle } from "react-icons/md";
import { useAuthStore } from "@/store/authStore";
import { useNavStore } from "@/store/navStore";
import { ROUTES } from "@/constants/routes";
import { useHasMounted } from "@/hooks/useHasMounted";
import Logo from "./Logo";

export default function Header() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const openDrawer = useNavStore((s) => s.openDrawer);
  // 서버 렌더·클라이언트 첫 렌더는 항상 비로그인 상태로 고정 — silent refresh로 복구된
  // accessToken이 하이드레이션 커밋보다 먼저 반영돼 발생하는 하이드레이션 불일치 방지
  const mounted = useHasMounted();

  return (
    <header className="sticky top-0 z-50 border-outline-variant bg-background">
      <div className="relative flex h-16 items-center justify-between px-8 gap-4">
        {/* Brand */}
        <div className="flex-1 flex items-center min-w-0">
          <button
            className="md:hidden p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
            onClick={openDrawer}
            aria-label="메뉴 열기"
          >
            <MdMenu size={24} />
          </button>
        </div>

        {/* Logo (모바일: 헤더 정중앙 고정 배치) */}
        <Link
          href={ROUTES.board.all}
          className="md:hidden absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2"
        >
          <Logo size="sm" asLink={false}/>
        </Link>

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
          {mounted && accessToken ? (
            <Link
              href={ROUTES.my}
              className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
              title="마이페이지"
            >
              <MdAccountCircle size={30} />
            </Link>
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
