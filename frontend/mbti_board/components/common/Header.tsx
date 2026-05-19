"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
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
    <header className="sticky top-0 z-50 border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4">
        <Link href="/" className="text-lg font-bold tracking-tight text-gray-900">
          MBTI Board
        </Link>

        <nav className="flex items-center gap-3">
          {accessToken ? (
            <>
              <Link
                href={ROUTES.my}
                className="text-sm text-gray-600 hover:text-gray-900"
              >
                마이페이지
              </Link>
              <button
                onClick={handleLogout}
                className="rounded-md bg-gray-100 px-3 py-1.5 text-sm hover:bg-gray-200"
              >
                로그아웃
              </button>
            </>
          ) : (
            <Link
              href={ROUTES.login}
              className="rounded-md bg-gray-900 px-3 py-1.5 text-sm text-white hover:bg-gray-700"
            >
              로그인
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
