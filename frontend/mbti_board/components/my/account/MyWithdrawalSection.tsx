"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "@/components/common/Toast";
import { sendWithdrawalOtp, withdraw } from "@/lib/api/user";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/authStore";
import InfoBanner from "@/components/common/InfoBanner";

const WITHDRAWAL_STEP = { INFO: "info", OTP: "otp" } as const;
type WithdrawalStep = null | typeof WITHDRAWAL_STEP[keyof typeof WITHDRAWAL_STEP];

export default function MyWithdrawalSection() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const setLoggedIn = useAuthStore((s) => s.setLoggedIn);

  const [withdrawalStep, setWithdrawalStep] = useState<WithdrawalStep>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpError, setOtpError] = useState("");

  const sendOtpMutation = useMutation({
    mutationFn: sendWithdrawalOtp,
    onSuccess: () => {
      setWithdrawalStep(WITHDRAWAL_STEP.OTP);
      setOtpCode("");
      setOtpError("");
    },
    onError: () => {
      toast.error("인증 이메일 전송에 실패했습니다.");
    },
  });

  const withdrawMutation = useMutation({
    mutationFn: () => withdraw(otpCode),
    onSuccess: () => {
      setWithdrawalStep(null);
      setLoggedIn(false);
      queryClient.clear();
      toast.success("탈퇴가 완료되었습니다.");
      router.replace("/");
    },
    onError: (error: unknown) => {
      const axiosError = error as { response?: { data?: { message?: string } } };
      const message = axiosError?.response?.data?.message ?? "조금 있다가 다시 시도해주세요.";
      setOtpError(message);
    },
  });

  const handleClose = () => {
    if (sendOtpMutation.isPending || withdrawMutation.isPending) return;
    setWithdrawalStep(null);
    setOtpCode("");
    setOtpError("");
    sendOtpMutation.reset();
    withdrawMutation.reset();
  };

  return (
    <>
      <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-4 flex items-center justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-foreground">회원 탈퇴</p>
          <p className="text-xs text-muted-foreground mt-0.5">
            탈퇴 시 모든 개인 정보가 삭제되고 복구할 수 없습니다.
          </p>
        </div>
        <button
          onClick={() => setWithdrawalStep(WITHDRAWAL_STEP.INFO)}
          className="shrink-0 rounded-lg border border-destructive/40 px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 transition-colors"
        >
          탈퇴하기
        </button>
      </div>

      {/* 탈퇴 안내 다이얼로그 (Step 1) */}
      <Dialog open={withdrawalStep === WITHDRAWAL_STEP.INFO} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>회원 탈퇴</DialogTitle>
            <DialogDescription>탈퇴 전 아래 내용을 확인해 주세요.</DialogDescription>
          </DialogHeader>
          <InfoBanner items={[
            { variant: "error", text: "탈퇴 후 모든 개인 정보가 삭제됩니다." },
            { variant: "info",  text: "작성하신 게시글과 댓글은 익명으로 남겨집니다." },
            { variant: "info",  text: "탈퇴 후 동일 이메일로 재가입이 가능합니다." },
          ]} />
          <DialogFooter className="flex-row gap-2">
            <button type="button" onClick={handleClose} disabled={sendOtpMutation.isPending} className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50">
              취소
            </button>
            <button type="button" onClick={() => sendOtpMutation.mutate()} disabled={sendOtpMutation.isPending} className="flex-1 rounded-lg bg-destructive py-2 text-sm text-white hover:opacity-90 disabled:opacity-50 transition-opacity">
              {sendOtpMutation.isPending ? "전송 중..." : "인증 이메일 전송"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* OTP 입력 다이얼로그 (Step 2) */}
      <Dialog open={withdrawalStep === WITHDRAWAL_STEP.OTP} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>탈퇴 인증</DialogTitle>
            <DialogDescription>가입한 이메일로 인증 코드가 전송되었습니다.</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <input
              type="text"
              value={otpCode}
              onChange={(e) => {
                setOtpCode(e.target.value.replace(/\D/g, "").slice(0, 6));
                setOtpError("");
              }}
              placeholder="6자리 인증 코드"
              maxLength={6}
              inputMode="numeric"
              className="w-full rounded-lg border border-outline-variant px-3 py-2 text-sm text-center tracking-widest focus:border-ring focus:outline-none"
            />
            {otpError && <p className="text-xs text-destructive">{otpError}</p>}
            <button type="button" onClick={() => sendOtpMutation.mutate()} disabled={sendOtpMutation.isPending} className="text-xs text-muted-foreground hover:text-foreground underline disabled:no-underline disabled:opacity-50">
              {sendOtpMutation.isPending ? "전송 중..." : "인증 이메일 재전송"}
            </button>
          </div>
          <DialogFooter className="flex-row gap-2">
            <button type="button" onClick={handleClose} disabled={withdrawMutation.isPending} className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50">
              취소
            </button>
            <button type="button" onClick={() => withdrawMutation.mutate()} disabled={withdrawMutation.isPending || otpCode.length < 6} className="flex-1 rounded-lg bg-destructive py-2 text-sm text-white hover:opacity-90 disabled:opacity-50 transition-opacity">
              {withdrawMutation.isPending ? "처리 중..." : "탈퇴 확인"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
