"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { FiLogOut } from "react-icons/fi";
import { logout } from "@/lib/api/auth";
import { ROUTES } from "@/constants/routes";
import { useAuthStore } from "@/store/authStore";

export default function LogoutButton({ className }: { className?: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const setLoggedIn = useAuthStore((s) => s.setLoggedIn);

  const handleLogout = async () => {
    // 소셜 로그인이었다면 AS 세션도 같이 끝내야 다음 로그인 때 (자동 재로그인 없이) 로그인 화면이 뜬다.
    let authServerLogoutUrl: string | null = null;
    try {
      ({ authServerLogoutUrl } = await logout());
    } finally {
      setLoggedIn(false);
      queryClient.clear();
      if (authServerLogoutUrl) {
        window.location.href = authServerLogoutUrl;
      } else {
        router.push(ROUTES.login);
      }
    }
  };

  return (
    <button onClick={handleLogout} className={className}>
      <FiLogOut className="w-3.5 h-3.5" />
      로그아웃
    </button>
  );
}
