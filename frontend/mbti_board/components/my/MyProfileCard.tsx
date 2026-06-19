"use client";

import { Suspense, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { getAllMbti, getMbtiById } from "@/constants/mbtiData";
import MbtiAvatar from "@/components/common/MbtiAvatar";
import { userKeys, useMyProfileSuspense } from "@/hooks/useUser";
import { isAxiosError } from "axios";
import MbtiSelect from "@/components/common/MbtiSelect";
import { updateMyProfile } from "@/lib/api/user";
import {
  Dialog,
  DialogContent,
  DialogClose,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { VisuallyHidden } from "@/components/ui/visually-hidden";
import { FiEdit2, FiMail, FiCalendar, FiUser } from "react-icons/fi";
import { HiInformationCircle } from "react-icons/hi";
import { LuSprout, LuX, LuBird } from "react-icons/lu";
import { MBTI_VERIFIED_META } from "@/types/user";
import { ROUTES } from "@/constants/routes";
import ErrorMessage from "@/components/common/ErrorMessage";
import InfoBanner from "@/components/common/InfoBanner";
import ClearableInput from "@/components/common/ClearableInput";
import Link from "next/link";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";

const EDITING_FIELD = { NICKNAME: "nickname", MBTI: "mbti" } as const;
type EditingField = null | typeof EDITING_FIELD[keyof typeof EDITING_FIELD];

function MyProfileCardInner() {
  const queryClient = useQueryClient();
  const [editingField, setEditingField] = useState<EditingField>(null);
  const [nickname, setNickname] = useState("");
  const [mbtiId, setMbtiId] = useState("");
  const [nicknameError, setNicknameError] = useState("");

  const { data: profile } = useMyProfileSuspense();
  const mbtiList = getAllMbti();

  const mutation = useMutation({
    mutationFn: updateMyProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(userKeys.myProfile(), updated);
      setNicknameError("");
      setEditingField(null);
    },
    onError: (err) => {
      const message = isAxiosError(err)
        ? (err.response?.data?.message ?? "저장에 실패했습니다. 다시 시도해 주세요.")
        : "저장에 실패했습니다. 다시 시도해 주세요.";
      setNicknameError(message);
    },
  });

  const openNicknameEdit = () => {
    setNickname(profile.nickname);
    setNicknameError("");
    mutation.reset();
    setEditingField(EDITING_FIELD.NICKNAME);
  };

  const openMbtiEdit = () => {
    setMbtiId(profile.mbtiId);
    mutation.reset();
    setEditingField(EDITING_FIELD.MBTI);
  };

  const closeEdit = () => {
    setEditingField(null);
    setNicknameError("");
    mutation.reset();
  };

  const handleNicknameSave = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = nickname.trim();
    if (trimmed === profile.nickname) { setEditingField(null); return; }
    if (trimmed.length < 2 || trimmed.length > 12) {
      setNicknameError("별명은 2~12자 이내로 입력해 주세요.");
      return;
    }
    if (!/^[가-힣a-zA-Z0-9_]+$/.test(trimmed)) {
      setNicknameError("한글, 영문, 숫자, 언더바(_)만 사용할 수 있습니다.");
      return;
    }
    mutation.mutate({ nickname: trimmed });
  };

  const handleMbtiSave = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (mbtiId === profile.mbtiId) { setEditingField(null); return; }
    mutation.mutate({ mbtiId });
  };

  return (
    <>
      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-5 shadow-sm relative overflow-hidden">
        <div className="flex flex-col items-center text-center relative z-10">
          <div className="-mb-8">
            <MbtiAvatar mbti={getMbtiById(profile.mbtiId)} size="xl" />
          </div>
          <div className="mt-5 w-full space-y-3">
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiUser className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left flex-1 min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">별명</p>
                <p className="text-sm font-semibold text-foreground truncate">{profile.nickname}</p>
              </div>
              <button onClick={openNicknameEdit} className="shrink-0 p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors">
                <FiEdit2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiMail className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">이메일</p>
                <p className="text-sm font-semibold text-foreground truncate">{profile.email}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <span className="text-[11px] font-bold text-muted-foreground shrink-0 w-4 text-center">M</span>
              <div className="text-left flex-1 min-w-0">
                <p className="text-sm font-semibold text-foreground">{profile.mbtiId}</p>
              </div>
              <span style={{ backgroundColor: MBTI_VERIFIED_META[profile.mbtiVerified].color }} className="shrink-0 rounded-md px-2 py-0.5 text-[11px] font-medium text-white">
                {MBTI_VERIFIED_META[profile.mbtiVerified].label}
              </span>
              <button onClick={openMbtiEdit} className="shrink-0 p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors">
                <FiEdit2 className="w-3.5 h-3.5" />
              </button>
            </div>
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiCalendar className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">가입일</p>
                <p className="text-sm text-foreground">{new Date(profile.createdAt).toLocaleDateString("ko-KR")}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 별명 편집 다이얼로그 */}
      <Dialog open={editingField === EDITING_FIELD.NICKNAME} onOpenChange={(open) => !open && closeEdit()}>
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
          <form onSubmit={handleNicknameSave} className="space-y-6">
            <div className="space-y-2">
              <fieldset className={`rounded-lg bg-background border px-3 pb-2 transition-all ${nicknameError ? "border-destructive" : "border-outline-variant focus-within:border-secondary focus-within:ring-4 focus-within:ring-secondary/10"}`}>
                <legend className="ml-1 px-1 text-xs font-semibold text-on-surface-variant">새 별명</legend>
                <ClearableInput
                  id="nickname-input"
                  value={nickname}
                  onChange={(e) => { setNickname(e.target.value); setNicknameError(""); }}
                  onClear={() => { setNickname(""); setNicknameError(""); }}
                  minLength={2}
                  maxLength={12}
                  required
                  autoFocus
                  placeholder="별명을 입력하세요"
                  inputClassName="w-full h-10 bg-transparent text-on-surface text-sm focus:outline-none placeholder:text-outline/50"
                />
              </fieldset>
              {nicknameError && <ErrorMessage message={nicknameError} />}
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
              <button type="button" onClick={closeEdit} disabled={mutation.isPending} className="h-12 px-6 sm:flex-1 rounded-full border border-outline text-primary text-sm font-semibold hover:bg-surface-container-low active:scale-95 transition-all disabled:opacity-50">
                취소
              </button>
              <button type="submit" disabled={mutation.isPending} className="h-12 px-6 sm:flex-[1.5] rounded-full bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 shadow-sm active:scale-95 transition-all disabled:opacity-50">
                {mutation.isPending ? "저장 중..." : "별명 변경"}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* MBTI 편집 다이얼로그 */}
      <Dialog open={editingField === EDITING_FIELD.MBTI} onOpenChange={(open) => !open && closeEdit()}>
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
          <form onSubmit={handleMbtiSave} className="flex flex-col">
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
                  <MbtiSelect value={mbtiId} onChange={(v) => setMbtiId(v ?? "")} mbtiList={mbtiList} />
                </div>
                {mbtiId && mbtiId !== profile.mbtiId && (
                  <InfoBanner items={[
                    { variant: "warning", text: "MBTI를 변경하면 -> 인증 상태가 '미인증'으로 초기화됩니다." },
                    { variant: "warning", text: "검사를 통해 변경하면 -> 인증 상태가 '인증'으로 초기화됩니다." },
                  ]} />
                )}
                {mutation.isError && (
                  <p className="text-xs text-destructive ml-1">저장에 실패했습니다. 다시 시도해 주세요.</p>
                )}
              </div>
              <div className="flex flex-col sm:flex-row gap-3">
                <button type="button" onClick={closeEdit} disabled={mutation.isPending} className="w-full sm:w-1/2 h-12 rounded-full border border-outline text-outline font-label-md text-label-md hover:bg-surface-container-low transition-all active:scale-95 disabled:opacity-50">
                  취소
                </button>
                <button type="submit" disabled={mutation.isPending} className="w-full sm:w-1/2 h-12 rounded-full bg-primary text-primary-foreground font-label-md text-label-md hover:shadow-md hover:bg-secondary/90 transition-all active:scale-95 disabled:opacity-50">
                  {mutation.isPending ? "저장 중..." : "변경하기"}
                </button>
              </div>
            </div>
            <div className="h-2 bg-linear-to-r from-primary via-secondary to-tertiary-fixed-dim" />
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}

export default function MyProfileCard() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={
        <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-5 shadow-sm animate-pulse">
          <div className="flex flex-col items-center gap-4">
            <div className="w-24 h-24 rounded-full bg-surface-container-low" />
            <div className="w-full space-y-3">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-14 rounded-xl bg-surface-container-low" />
              ))}
            </div>
          </div>
        </div>
      }>
        <MyProfileCardInner />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
