"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ROUTES } from "@/lib/routes";

const BOARD_TABS = [
  { label: "자유 게시판", href: ROUTES.board.free },
  { label: "투표 게시판", href: ROUTES.board.vote },
  { label: "토론 게시판", href: ROUTES.board.debate },
] as const;

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-48 shrink-0 border-r border-gray-200 bg-white">
      <nav className="p-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-gray-400">
          게시판
        </p>
        <ul className="space-y-1">
          {BOARD_TABS.map(({ label, href }) => {
            const isActive = pathname.startsWith(href);
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
