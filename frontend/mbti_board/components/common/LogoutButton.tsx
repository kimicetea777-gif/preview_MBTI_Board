"use client";

import { useRouter } from "next/navigation";
import { FiLogOut } from "react-icons/fi";
import { apiClient } from "@/lib/apiClient";
import { API } from "@/constants/apiEndpoints";
import { ROUTES } from "@/constants/routes";
import { useAuthStore } from "@/store/authStore";

export default function LogoutButton({ className }: { className?: string }) {
  const router = useRouter();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const handleLogout = async () => {
    try {
      await apiClient.post(API.auth.logout);
    } finally {
      setAccessToken(null);
      router.push(ROUTES.login);
    }
  };

  return (
    <button onClick={handleLogout} className={className}>
      <FiLogOut className="w-3.5 h-3.5" />
      로그아웃
    </button>
  );
}
