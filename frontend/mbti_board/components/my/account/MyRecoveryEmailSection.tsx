"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { toast } from "@/components/common/Toast";
import { sendRecoveryEmailVerification, removeRecoveryEmail } from "@/lib/api/user";
import { userKeys } from "@/hooks/useUser";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import InfoBanner from "@/components/common/InfoBanner";
import { LuMail } from "react-icons/lu";
import { EMAIL_REGEX } from "@/lib/validationConstants";

interface Props {
  hasRecoveryEmail: boolean;
  loginEmail: string;
}

type DialogType = "edit" | "delete" | null;

export default function MyRecoveryEmailSection({ hasRecoveryEmail, loginEmail }: Props) {
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<DialogType>(null);
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState("");

  const isSameAsLogin = email.length > 0 && email.toLowerCase() === loginEmail.toLowerCase();
  const isEmailValid = EMAIL_REGEX.test(email) && !isSameAsLogin;

  const closeDialog = () => {
    setDialog(null);
    setEmail("");
    setEmailError("");
    sendMutation.reset();
    deleteMutation.reset();
  };

  const sendMutation = useMutation({
    mutationFn: () => sendRecoveryEmailVerification({ email }),
    onSuccess: () => {
      toast.success("인증 메일을 발송했습니다. 메일함을 확인해주세요.");
      closeDialog();
    },
    onError: (error: unknown) => {
      const message = isAxiosError(error)
        ? (error.response?.data?.message ?? "발송에 실패했습니다. 다시 시도해 주세요.")
        : "발송에 실패했습니다. 다시 시도해 주세요.";
      setEmailError(message);
      toast.error(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: removeRecoveryEmail,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
      toast.success("복구 이메일이 삭제되었습니다.");
      closeDialog();
    },
    onError: () => {
      toast.error("삭제에 실패했습니다. 다시 시도해 주세요.");
    },
  });

  return (
    <>
      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
        <div className="flex items-center gap-2 mb-1">
          <LuMail className="text-muted-foreground" size={18} />
          <h2 className="font-heading text-base font-bold text-foreground">복구 이메일</h2>
        </div>
        <p className="text-xs text-muted-foreground mb-5">이메일 찾기에 사용되는 인증된 이메일입니다.</p>

        <div className="flex items-center justify-between gap-4 rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3">
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center rounded-full px-4 py-2 text-xs font-semibold ${
              hasRecoveryEmail
                ? "bg-emerald-500/10 text-emerald-600"
                : "bg-muted text-muted-foreground"
            }`}>
              {hasRecoveryEmail ? "등록됨" : "미등록"}
            </span>
            {!hasRecoveryEmail && (
              <span className="text-xs text-muted-foreground">복구 이메일을 등록하면 이메일을 찾을 수 있습니다.</span>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => { setEmail(""); setDialog("edit"); }}
              className="rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent transition-colors"
            >
              {hasRecoveryEmail ? "변경하기" : "등록하기"}
            </button>
            {hasRecoveryEmail && (
              <button
                onClick={() => setDialog("delete")}
                className="rounded-lg border border-destructive/40 px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 transition-colors"
              >
                삭제
              </button>
            )}
          </div>
        </div>
      </div>

      {/* 등록/변경 다이얼로그 */}
      <Dialog open={dialog === "edit"} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>복구 이메일 {hasRecoveryEmail ? "변경" : "등록"}</DialogTitle>
            <DialogDescription>
              입력한 이메일로 인증 링크를 보내드립니다. 링크를 클릭해야 등록이 완료됩니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <input
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setEmailError(""); }}
              placeholder="recovery@example.com"
              className="w-full rounded-lg border border-outline-variant px-3 py-2 text-sm focus:border-ring focus:outline-none"
            />
            {isSameAsLogin && <p className="text-xs text-destructive">로그인 이메일과 다른 이메일을 입력해 주세요.</p>}
            {!isSameAsLogin && emailError && <p className="text-xs text-destructive">{emailError}</p>}
            <InfoBanner variant="info" text="로그인 이메일과 다른 이메일을 등록해야 합니다." />
          </div>
          <DialogFooter className="flex-row gap-2">
            <button type="button" onClick={closeDialog} disabled={sendMutation.isPending} className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50">
              취소
            </button>
            <button
              type="button"
              onClick={() => sendMutation.mutate()}
              disabled={sendMutation.isPending || !isEmailValid}
              className="flex-1 rounded-lg bg-primary py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-opacity"
            >
              {sendMutation.isPending ? "발송 중..." : "인증 메일 발송"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 삭제 확인 다이얼로그 */}
      <Dialog open={dialog === "delete"} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>복구 이메일 삭제</DialogTitle>
            <DialogDescription>
              복구 이메일을 삭제하면 이메일 찾기를 이용할 수 없게 됩니다.
            </DialogDescription>
          </DialogHeader>
          <InfoBanner variant="warning" text="삭제 후 이메일 찾기가 불가합니다." />
          <DialogFooter className="flex-row gap-2">
            <button type="button" onClick={closeDialog} disabled={deleteMutation.isPending} className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50">
              취소
            </button>
            <button
              type="button"
              onClick={() => deleteMutation.mutate()}
              disabled={deleteMutation.isPending}
              className="flex-1 rounded-lg bg-destructive py-2 text-sm text-white hover:opacity-90 disabled:opacity-50 transition-opacity"
            >
              {deleteMutation.isPending ? "삭제 중..." : "삭제 확인"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
