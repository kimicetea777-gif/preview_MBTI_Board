"use client";

import Link from "next/link";
import { LuBookMarked, LuChartBar } from "react-icons/lu";
import { ROUTES } from "@/constants/routes";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { MbtiDetail } from "./MbtiDetail";

interface Props {
  mbtiId: string;
  saved: boolean;
  isSubmitting: boolean;
  isSubmitError: boolean;
  saveDialogOpen: boolean;
  onSaveOpen: () => void;
  onSaveClose: () => void;
  onSaveConfirm: () => void;
}

export function MbtiResultPhase({
  mbtiId,
  saved,
  isSubmitting,
  isSubmitError,
  saveDialogOpen,
  onSaveOpen,
  onSaveClose,
  onSaveConfirm,
}: Props) {
  const saveSlot = (
    <div className="flex flex-col items-center gap-2">
      <Link
        href={ROUTES.mbti.funStats}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <LuChartBar className="text-base" />
        다른 사람들의 선택 보기 →
      </Link>
      {!saved && (
        <button
          onClick={onSaveOpen}
          disabled={isSubmitting}
          className="flex items-center gap-2 rounded-xl bg-primary px-5 py-3.5 text-sm font-semibold text-primary-foreground hover:bg-primary/80 disabled:opacity-50 transition-colors shadow-sm"
        >
          <LuBookMarked className="text-base" />
          MBTI 기록하기
        </button>
      )}
      {saved && (
        <div className="flex items-center gap-2 rounded-xl bg-primary px-5 py-3.5 text-sm font-semibold text-primary-foreground shadow-sm pointer-events-none">
          ✓ 기록되었습니다.
        </div>
      )}
      {isSubmitting && !saved && (
        <p className="text-sm text-muted-foreground">저장 중...</p>
      )}
      {isSubmitError && !saved && (
        <div className="flex items-center gap-2">
          <p className="text-sm text-destructive">저장에 실패했습니다.</p>
          <button
            onClick={onSaveConfirm}
            disabled={isSubmitting}
            className="text-sm text-primary hover:underline disabled:opacity-50"
          >
            다시 시도
          </button>
        </div>
      )}
    </div>
  );

  return (
    <>
      <MbtiDetail
        mbtiId={mbtiId}
        actionSlot={saveSlot}
      />

      <Dialog open={saveDialogOpen} onOpenChange={onSaveClose}>
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>MBTI를 기록하시겠어요?</DialogTitle>
            <DialogDescription>
              {mbtiId} 유형이 마이페이지에 저장되고 검사 인증 배지가 활성화됩니다.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <button
              onClick={onSaveClose}
              disabled={isSubmitting}
              className="flex-1 rounded-lg border border-border py-2 text-sm hover:bg-accent disabled:opacity-50 transition-colors"
            >
              아니요
            </button>
            <button
              onClick={onSaveConfirm}
              disabled={isSubmitting}
              className="flex-1 rounded-lg bg-primary py-2 text-sm text-primary-foreground hover:bg-primary/80 disabled:opacity-50 transition-colors"
            >
              {isSubmitting ? "저장 중..." : "네, 기록할게요"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
