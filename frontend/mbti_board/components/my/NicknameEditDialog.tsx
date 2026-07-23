"use client";

import { Dialog, DialogContent, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { FiUser } from "react-icons/fi";
import { HiInformationCircle } from "react-icons/hi";
import ErrorMessage from "@/components/common/ErrorMessage";
import InfoBanner from "@/components/common/InfoBanner";
import ClearableInput from "@/components/common/ClearableInput";

interface NicknameEditDialogProps {
  open: boolean;
  nickname: string;
  onNicknameChange: (value: string) => void;
  nicknameError: string;
  nicknameAvailable: boolean;
  nicknameTaken: boolean;
  isPending: boolean;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  onClose: () => void;
}

export default function NicknameEditDialog({
  open,
  nickname,
  onNicknameChange,
  nicknameError,
  nicknameAvailable,
  nicknameTaken,
  isPending,
  onSubmit,
  onClose,
}: NicknameEditDialogProps) {
  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent showCloseButton={false} className="sm:max-w-120 p-8 gap-0">
        <div className="mb-6">
          <div className="flex items-center gap-2 mb-2">
            <FiUser className="w-5 h-5 text-muted-foreground shrink-0" />
            <DialogTitle className="text-2xl font-semibold text-primary">별명 변경</DialogTitle>
          </div>
          <DialogDescription className="text-sm text-on-surface-variant leading-relaxed">
            별명 세탁(?) 어떤 별명을 지어볼까요?
          </DialogDescription>
        </div>
        <form onSubmit={onSubmit} className="space-y-6">
          <div className="space-y-2">
            <fieldset className={`rounded-lg bg-background border px-3 pb-2 transition-all ${nicknameError || nicknameTaken ? "border-destructive" : "border-outline-variant focus-within:border-secondary focus-within:ring-4 focus-within:ring-secondary/10"}`}>
              <legend className="ml-1 px-1 text-xs font-semibold text-on-surface-variant">새 별명</legend>
              <ClearableInput
                id="nickname-input"
                value={nickname}
                onChange={(e) => onNicknameChange(e.target.value)}
                onClear={() => onNicknameChange("")}
                minLength={2}
                maxLength={12}
                required
                autoFocus
                placeholder="별명을 입력하세요"
                inputClassName="w-full h-10 bg-transparent text-on-surface text-sm focus:outline-none placeholder:text-outline/50"
              />
            </fieldset>
            <div className="min-h-10">
              {nicknameError ? (
                <ErrorMessage message={nicknameError} />
              ) : nicknameTaken ? (
                <ErrorMessage message="이미 사용 중인 닉네임입니다." />
              ) : nicknameAvailable && (
                <InfoBanner variant="success" text="사용 가능한 닉네임입니다." />
              )}
            </div>
            <p className="text-xs text-outline flex items-center gap-1.5 ml-3.5">
              <HiInformationCircle className="text-sm shrink-0" />
              한글·영문·숫자·언더바(_)만 사용 가능
            </p>
            <p className="text-xs text-outline flex items-center gap-1.5 ml-3.5">
              <HiInformationCircle className="text-sm shrink-0" />
              2~12글자만 가능
            </p>
          </div>
          <div className="flex flex-col-reverse sm:flex-row gap-3">
            <button type="button" onClick={onClose} disabled={isPending} className="h-12 px-6 sm:flex-1 rounded-full border border-outline text-primary text-sm font-semibold hover:bg-surface-container-low active:scale-95 transition-all disabled:opacity-50">
              취소
            </button>
            <button type="submit" disabled={isPending || nicknameTaken} className="h-12 px-6 sm:flex-[1.5] rounded-full bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 shadow-sm active:scale-95 transition-all disabled:opacity-50">
              {isPending ? "저장 중..." : "별명 변경"}
            </button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
