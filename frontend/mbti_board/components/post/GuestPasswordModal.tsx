"use client";

import { useState } from "react";
import Link from "next/link";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { GUEST_PASSWORD_MIN_LENGTH, GUEST_PASSWORD_MAX_LENGTH } from "@/types/post";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import ErrorMessage from "@/components/common/ErrorMessage";
import { ROUTES } from "@/constants/routes";
import { toast } from "@/components/common/Toast";

export const MODAL_ACTION = { CREATE: "create", UPDATE: "update", DELETE: "delete" } as const;
export type ModalAction = (typeof MODAL_ACTION)[keyof typeof MODAL_ACTION];

const ACTION_LABEL: Record<ModalAction, string> = {
  create: "작성",
  update: "수정",
  delete: "삭제",
};

const DEFAULT_DESCRIPTION: Record<ModalAction, string> = {
  create: "비회원 비밀번호를 설정하세요. 댓글 수정·삭제 시 필요합니다.",
  update: "게시글 작성 시 입력한 비밀번호를 입력해 주세요.",
  delete: "게시글 작성 시 입력한 비밀번호를 입력해 주세요.",
};

export interface GuestAgreement {
  termsAgreed: boolean;
  privacyAgreed: boolean;
  age14Agreed: boolean;
}

interface Props {
  open: boolean;
  action: ModalAction;
  onConfirm: (password: string, agreement?: GuestAgreement) => void;
  onClose: () => void;
  isPending: boolean;
  error?: string;
  description?: string;
}

export default function GuestPasswordModal({
  open,
  action,
  onConfirm,
  onClose,
  isPending,
  error,
  description,
}: Props) {
  const [password, setPassword] = useState("");
  const [termsAgreed, setTermsAgreed] = useState(false);
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [age14Agreed, setAge14Agreed] = useState(false);

  const isCreate = action === MODAL_ACTION.CREATE;
  const isPasswordValid = password.length >= GUEST_PASSWORD_MIN_LENGTH;

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      setPassword("");
      setTermsAgreed(false);
      setPrivacyAgreed(false);
      setAge14Agreed(false);
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>(비회원) {ACTION_LABEL[action]} 확인</DialogTitle>
          <DialogDescription>
            {description ?? DEFAULT_DESCRIPTION[action]}
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (!isPasswordValid) return;
            if (isCreate && (!termsAgreed || !privacyAgreed || !age14Agreed)) {
              toast.warning("약관 동의를 읽고 체크해주세요.");
              return;
            }
            onConfirm(password, isCreate ? { termsAgreed, privacyAgreed, age14Agreed } : undefined);
          }}
          className="space-y-3"
        >
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호 (4자 이상)"
            autoComplete="off"
            minLength={GUEST_PASSWORD_MIN_LENGTH}
            maxLength={GUEST_PASSWORD_MAX_LENGTH}
            disabled={isPending}
          />
          {isCreate && (
            <div className="space-y-2 pt-1">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={termsAgreed}
                  onChange={(e) => setTermsAgreed(e.target.checked)}
                  disabled={isPending}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
                />
                <span className="text-xs text-on-surface-variant leading-relaxed">
                  <Link href={ROUTES.terms} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">이용약관</Link>에 동의합니다. <span className="text-red-500">*</span>
                </span>
              </label>
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={privacyAgreed}
                  onChange={(e) => setPrivacyAgreed(e.target.checked)}
                  disabled={isPending}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
                />
                <span className="text-xs text-on-surface-variant leading-relaxed">
                  <Link href={ROUTES.privacy} target="_blank" className="font-medium text-primary underline underline-offset-2 hover:opacity-80">개인정보처리방침</Link>에 동의합니다. <span className="text-red-500">*</span>
                </span>
              </label>
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={age14Agreed}
                  onChange={(e) => setAge14Agreed(e.target.checked)}
                  disabled={isPending}
                  className="mt-0.5 h-4 w-4 shrink-0 accent-primary"
                />
                <span className="text-xs text-on-surface-variant leading-relaxed">
                  본인은 만 14세 이상입니다. <span className="text-red-500">*</span>
                </span>
              </label>
            </div>
          )}
          <ErrorMessage message={error} />

          <DialogFooter className="flex-row">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isPending}
              className="flex-1"
            >
              취소
            </Button>
            <Button
              type="submit"
              disabled={isPending || !isPasswordValid}
              className="flex-1"
            >
              {isPending ? "처리 중..." : ACTION_LABEL[action]}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
