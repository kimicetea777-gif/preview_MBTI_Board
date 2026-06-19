"use client";

import { useState } from "react";
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

interface Props {
  open: boolean;
  action: ModalAction;
  onConfirm: (password: string) => void;
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

  const isPasswordValid = password.length >= GUEST_PASSWORD_MIN_LENGTH;

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      setPassword("");
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
            onConfirm(password);
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
          <ErrorMessage message={error} />

          <DialogFooter>
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
