"use client";

import Link from "next/link";
import { useAuthStore } from "@/store/authStore";

interface BoardHeaderProps {
  title: string;
  writeHref: string;
  requireAuth?: boolean;
}

export default function BoardHeader({ title, writeHref, requireAuth }: BoardHeaderProps) {
  const isLoggedIn = useAuthStore((s) => s.accessToken) !== null;
  const disabled = requireAuth && !isLoggedIn;

  return (
    <div className="flex items-center justify-between mb-2">
      <h1 className="text-xl font-bold text-gray-900">{title}</h1>
      {disabled ? (
        <button
          type="button"
          disabled
          title="로그인이 필요합니다."
          className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white opacity-40 cursor-not-allowed"
        >
          글쓰기
        </button>
      ) : (
        <Link
          href={writeHref}
          className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          글쓰기
        </Link>
      )}
    </div>
  );
}