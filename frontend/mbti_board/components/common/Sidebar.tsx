"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ROUTES } from "@/lib/routes";

const BOARD_TABS = [
  { label: "자유 게시판", href: ROUTES.board.free },
  { label: "투표 게시판", href: ROUTES.board.vote },
  { label: "토론 게시판", href: ROUTES.board.debate },
] as const;

const MY_TABS = [
  { label: "프로필", href: ROUTES.my },
] as const;

const MBTI_TABS = [
  { label: "MBTI 유형", href: ROUTES.mbti.list },
  { label: "MBTI 검사", href: ROUTES.mbti.test },
] as const;

export default function Sidebar() {
  const pathname = usePathname();
  const isMyPage = pathname.startsWith(ROUTES.my);
  const isMbtiPage = pathname.startsWith(ROUTES.mbti.list);
  const tabs = isMyPage ? MY_TABS : isMbtiPage ? MBTI_TABS : BOARD_TABS;
  const sectionLabel = isMyPage ? "마이페이지" : isMbtiPage ? "MBTI" : "게시판";

  return (
    <aside className="w-48 shrink-0 border-r border-gray-200 bg-white">
      <nav className="p-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-400">
          {sectionLabel}
        </p>
        <ul className="space-y-1">
          {tabs.map(({ label, href }) => {
            const isActive = pathname === href || (href !== ROUTES.my && href !== ROUTES.mbti.list && pathname.startsWith(href));
            return (
              <li key={href}>
                <Link
                  href={href}
                  className={`block rounded-md px-3 py-2 text-sm ${
                    isActive
                      ? "bg-gray-100 font-medium text-gray-900"
                      : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                  }`}
                >
                  {label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </aside>
  );
}
