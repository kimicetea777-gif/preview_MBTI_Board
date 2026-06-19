"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { toast } from "@/components/common/Toast";
import { updatePhone, removePhone } from "@/lib/api/user";
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
import { LuPhone } from "react-icons/lu";
import { PHONE_REGEX } from "@/lib/validationConstants";
import { usePhoneInput } from "@/hooks/usePhoneInput";

interface Props {
  hasPhone: boolean;
}

type DialogType = "edit" | "delete" | null;

export default function MyPhoneSection({ hasPhone }: Props) {
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<DialogType>(null);
  const { phone, setPhone, handlePhoneChange } = usePhoneInput();
  const [phoneError, setPhoneError] = useState("");


  const isPhoneValid = PHONE_REGEX.test(phone);

  const closeDialog = () => {
    setDialog(null);
    setPhone("");
    setPhoneError("");
    updateMutation.reset();
    deleteMutation.reset();
  };

  const updateMutation = useMutation({
    mutationFn: () => updatePhone(phone.replace(/-/g, "")),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
      toast.success(hasPhone ? "전화번호가 변경되었습니다." : "전화번호가 등록되었습니다.");
      closeDialog();
    },
    onError: (error: unknown) => {
      const message = isAxiosError(error)
        ? (error.response?.data?.message ?? "저장에 실패했습니다. 다시 시도해 주세요.")
        : "저장에 실패했습니다. 다시 시도해 주세요.";
      setPhoneError(message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: removePhone,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.myProfile() });
      toast.success("전화번호가 삭제되었습니다.");
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
          <LuPhone className="text-muted-foreground" size={18} />
          <h2 className="font-heading text-base font-bold text-foreground">전화번호</h2>
        </div>
        <p className="text-xs text-muted-foreground mb-5">이메일 찾기에 사용되는 전화번호입니다.</p>

        <div className="flex items-center justify-between gap-4 rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3">
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center rounded-full px-4 py-2 text-xs font-semibold ${
              hasPhone
                ? "bg-emerald-500/10 text-emerald-600"
                : "bg-muted text-muted-foreground"
            }`}>
              {hasPhone ? "등록됨" : "미등록"}
            </span>
            {!hasPhone && (
              <span className="text-xs text-muted-foreground">전화번호를 등록하면 이메일을 찾을 수 있습니다.</span>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => { setPhone(""); setDialog("edit"); }}
              className="rounded-lg border border-outline-variant px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent transition-colors"
            >
              {hasPhone ? "변경하기" : "등록하기"}
            </button>
            {hasPhone && (
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
            <DialogTitle>전화번호 {hasPhone ? "변경" : "등록"}</DialogTitle>
            <DialogDescription>
              전화번호는 이메일 찾기에만 사용되며, 암호화되어 저장됩니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <input
              type="tel"
              value={phone}
              onChange={(e) => { handlePhoneChange(e); setPhoneError(""); }}
              placeholder="010-0000-0000"
              inputMode="numeric"
              className="w-full rounded-lg border border-outline-variant px-3 py-2 text-sm focus:border-ring focus:outline-none"
            />
            {phoneError && <p className="text-xs text-destructive">{phoneError}</p>}
            <InfoBanner variant="info" text="입력한 전화번호 원본은 저장되지 않아 조회가 불가합니다." />
          </div>
          <DialogFooter className="flex-row gap-2">
            <button type="button" onClick={closeDialog} disabled={updateMutation.isPending} className="flex-1 rounded-lg border border-outline-variant py-2 text-sm hover:bg-accent disabled:opacity-50">
              취소
            </button>
            <button
              type="button"
              onClick={() => updateMutation.mutate()}
              disabled={updateMutation.isPending || !isPhoneValid}
              className="flex-1 rounded-lg bg-primary py-2 text-sm text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-opacity"
            >
              {updateMutation.isPending ? "저장 중..." : "저장"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 삭제 확인 다이얼로그 */}
      <Dialog open={dialog === "delete"} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>전화번호 삭제</DialogTitle>
            <DialogDescription>
              전화번호를 삭제하면 이메일 찾기를 이용할 수 없게 됩니다.
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
