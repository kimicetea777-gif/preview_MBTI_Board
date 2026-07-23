"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "@/components/common/Toast";
import { logoutAll } from "@/lib/api/auth";
import { ROUTES } from "@/constants/routes";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/authStore";

export default function MyLogoutAllSection() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const [showConfirm, setShowConfirm] = useState(false);

  const logoutAllMutation = useMutation({
    mutationFn: logoutAll,
    onSuccess: () => {
      setShowConfirm(false);
      setAccessToken(null);
      queryClient.clear();
      toast.success("모든 기기에서 로그아웃되었습니다.");
      router.push(ROUTES.login);
    },
    onError: () => {
      toast.error("잠시 후 다시 시도해주세요.");
    },
  });

  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
      <h2 className="font-heading text-base font-bold text-foreground mb-4">기기 관리</h2>
      <div className="rounded-xl border border-outline-variant/40 bg-surface-container-low p-4 flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-foreground">연동 기기 전체 로그아웃</p>
          <p className="text-xs text-muted-foreground mt-0.5">
            공용 PC 등 다른 기기에 남아있는 로그인 세션을 모두 종료합니다.
          </p>
        </div>
        <button
          onClick={() => setShowConfirm(true)}
          className="shrink-0 rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent transition-colors"
        >
          전체 로그아웃
        </button>
      </div>

      <Dialog open={showConfirm} onOpenChange={(open) => !open && !logoutAllMutation.isPending && setShowConfirm(false)}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>전체 기기 로그아웃</DialogTitle>
            <DialogDescription>
              현재 기기를 포함해 로그인된 모든 기기에서 로그아웃됩니다. 계속하시겠습니까?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={() => setShowConfirm(false)}
              disabled={logoutAllMutation.isPending}
              className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={() => logoutAllMutation.mutate()}
              disabled={logoutAllMutation.isPending}
              className="flex-1 rounded-lg bg-destructive py-2 text-sm text-white hover:opacity-90 disabled:opacity-50 transition-opacity"
            >
              {logoutAllMutation.isPending ? "처리 중..." : "로그아웃"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
