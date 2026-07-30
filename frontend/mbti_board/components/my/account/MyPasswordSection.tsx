"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/components/common/Toast";
import { requestPasswordReset } from "@/lib/api/auth";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import InfoBanner from "@/components/common/InfoBanner";
import { SENDER_EMAIL } from "@/constants/site";
import { LuKeyRound } from "react-icons/lu";

interface Props {
  isSocialOnly: boolean;
  email: string;
}

type DialogStep = "confirm" | "sent" | null;

export default function MyPasswordSection({ isSocialOnly, email }: Props) {
  const [dialog, setDialog] = useState<DialogStep>(null);

  const mutation = useMutation({
    mutationFn: () => requestPasswordReset({ email }),
    onSuccess: () => setDialog("sent"),
    onError: () => toast.error("이메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
  });

  const handleClose = () => {
    if (mutation.isPending) return;
    setDialog(null);
    mutation.reset();
  };

  return (
    <>
      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
        <h2 className="font-heading text-base font-bold text-foreground mb-3 flex items-center gap-1.5">
          <LuKeyRound className="text-lg" />
          비밀번호 변경
        </h2>
        {isSocialOnly ? (
          <InfoBanner variant="info" text="소셜 로그인 계정은 비밀번호 변경이 지원되지 않습니다." />
        ) : (
          <div className="flex items-center justify-between gap-4 rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3">
            <span className="text-xs text-muted-foreground">이메일 인증 링크를 통해 새 비밀번호를 설정합니다.</span>
            <button
              onClick={() => setDialog("confirm")}
              className="shrink-0 rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent transition-colors"
            >
              변경하기
            </button>
          </div>
        )}
      </div>

      {/* 확인 다이얼로그 */}
      <Dialog open={dialog === "confirm"} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>비밀번호 재설정</DialogTitle>
            <DialogDescription>
              아래 이메일로 비밀번호 재설정 링크를 보내드립니다.
            </DialogDescription>
          </DialogHeader>
          <InfoBanner items={[
            { variant: "info",    text: `전송 이메일: ${email}` },
            { variant: "warning", text: "링크는 30분간 유효합니다." },
          ]} />
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={handleClose}
              disabled={mutation.isPending}
              className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={() => mutation.mutate()}
              disabled={mutation.isPending}
              className="flex-1 rounded-lg bg-primary py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-opacity"
            >
              {mutation.isPending ? "전송 중..." : "인증 이메일 전송"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 전송 완료 다이얼로그 */}
      <Dialog open={dialog === "sent"} onOpenChange={(open) => !open && handleClose()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>이메일 전송 완료</DialogTitle>
            <DialogDescription>{email}로 비밀번호 재설정 링크가 전송되었습니다.</DialogDescription>
          </DialogHeader>
          <InfoBanner items={[
            { variant: "info",    text: "이메일의 링크를 클릭해 새 비밀번호를 설정하세요" },
            { variant: "info",    text: `이메일이 없다면 스팸함을 확인해 주세요` },
            { variant: "warning", text: "링크는 30분간 유효합니다" },
            { variant: "error",    text: `발신자가 ${SENDER_EMAIL}인지 확인하세요` },
          ]} />
          <DialogFooter>
            <button
              type="button"
              onClick={handleClose}
              className="w-full rounded-lg bg-primary py-2 text-sm text-primary-foreground hover:bg-primary/90 transition-opacity"
            >
              확인
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
