"use client";

import { Dialog, DialogContent, DialogClose, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { VisuallyHidden } from "@/components/ui/visually-hidden";
import { LuSprout, LuX, LuBird } from "react-icons/lu";
import Link from "next/link";
import { ROUTES } from "@/constants/routes";
import MbtiSelect from "@/components/common/MbtiSelect";
import InfoBanner from "@/components/common/InfoBanner";

interface MbtiEditDialogProps {
  open: boolean;
  mbtiId: string;
  onMbtiIdChange: (value: string) => void;
  currentMbtiId: string;
  isPending: boolean;
  isError: boolean;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onClose: () => void;
}

export default function MbtiEditDialog({
  open,
  mbtiId,
  onMbtiIdChange,
  currentMbtiId,
  isPending,
  isError,
  onSubmit,
  onClose,
}: MbtiEditDialogProps) {
  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent showCloseButton={false} className="sm:max-w-lg p-0 gap-0 overflow-hidden rounded-[1.5rem] border border-outline-variant/40 shadow-[0_20px_40px_-15px_rgba(49,23,10,0.15)]">
        <VisuallyHidden>
          <DialogTitle>MBTI 타입 변경</DialogTitle>
          <DialogDescription>MBTI 타입을 검사하거나 직접 선택해 변경합니다.</DialogDescription>
        </VisuallyHidden>
        <DialogClose asChild>
          <button className="absolute top-4 right-4 p-2 rounded-full hover:bg-surface-container text-on-surface-variant transition-colors z-10">
            <LuX className="w-5 h-5" />
          </button>
        </DialogClose>
        <form onSubmit={onSubmit} className="flex flex-col">
          <div className="p-8 md:p-10">
            <div className="text-center mb-6">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-secondary-fixed mb-4 text-on-secondary-container">
                <LuBird className="w-8 h-8" />
              </div>
              <h2 className="text-2xl font-bold text-primary mb-2">MBTI 타입 변경</h2>
              <p className="text-sm text-on-surface-variant leading-relaxed">
                새로운 MBTI를 선택하거나, 검사를 다시 받아 새로운 사람(?)이 되어보아요.
              </p>
            </div>
            <div className="flex justify-center mb-6">
              <Link href={ROUTES.mbti.test} className="inline-flex items-center gap-2 rounded-lg border border-ring px-5 py-2.5 text-sm font-semibold text-ring transition-colors hover:bg-ring/10">
                <LuSprout className="h-4 w-4" />
                MBTI 검사 받고 변경하기
              </Link>
            </div>
            <div className="relative flex items-center mb-8">
              <div className="grow border-t border-outline-variant/50" />
              <span className="px-4 text-sm font-medium text-outline uppercase tracking-widest">또는</span>
              <div className="grow border-t border-outline-variant/50" />
            </div>
            <div className="mb-8 space-y-2">
              <div className="flex items-center gap-3">
                <label className="shrink-0 text-sm font-semibold text-primary">직접 선택 : </label>
                <MbtiSelect value={mbtiId} onChange={(v) => onMbtiIdChange(v ?? "")} />
              </div>
              {mbtiId && mbtiId !== currentMbtiId && (
                <InfoBanner items={[
                  { variant: "warning", text: "MBTI를 변경하면 -> 인증 상태가 '미인증'으로 초기화됩니다." },
                  { variant: "warning", text: "검사를 통해 변경하면 -> 인증 상태가 '인증'으로 초기화됩니다." },
                ]} />
              )}
              {isError && (
                <p className="text-xs text-destructive ml-1">저장에 실패했습니다. 다시 시도해 주세요.</p>
              )}
            </div>
            <div className="flex flex-col sm:flex-row gap-3">
              <button type="button" onClick={onClose} disabled={isPending} className="w-full sm:w-1/2 h-12 rounded-full border border-outline text-outline font-label-md text-label-md hover:bg-surface-container-low transition-all active:scale-95 disabled:opacity-50">
                취소
              </button>
              <button type="submit" disabled={isPending} className="w-full sm:w-1/2 h-12 rounded-full bg-primary text-primary-foreground font-label-md text-label-md hover:shadow-md hover:bg-secondary/90 transition-all active:scale-95 disabled:opacity-50">
                {isPending ? "저장 중..." : "변경하기"}
              </button>
            </div>
          </div>
          <div className="h-2 bg-linear-to-r from-primary via-secondary to-tertiary-fixed-dim" />
        </form>
      </DialogContent>
    </Dialog>
  );
}
