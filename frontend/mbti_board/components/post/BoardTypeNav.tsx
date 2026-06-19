"use client";

import { Suspense } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { ROUTES } from "@/constants/routes";

const TABS = [
  { label: "전체",   href: ROUTES.board.all,    type: null },
  { label: "자유글", href: ROUTES.board.free,   type: "FREE" },
  { label: "투표글", href: ROUTES.board.vote,   type: "VOTE" },
  { label: "토론글", href: ROUTES.board.debate, type: "DEBATE" },
] as const;

function BoardTypeNavInner() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentType = searchParams.get("type");

  return (
    <div className="flex w-fit rounded-lg border border-gray-200 overflow-hidden text-xs">
      {TABS.map((tab) => {
        const isActive = pathname === "/board" && currentType === tab.type;
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`px-3 py-1.5 transition-colors ${
              isActive
                ? "bg-gray-900 text-white"
                : "bg-white text-gray-600 hover:bg-gray-50"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </div>
  );
}

export default function BoardTypeNav() {
  return (
    <Suspense>
      <BoardTypeNavInner />
    </Suspense>
  );
}
