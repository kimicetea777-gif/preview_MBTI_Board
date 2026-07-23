"use client";

import { Suspense, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { getMbtiById } from "@/constants/mbtiData";
import MbtiAvatar from "@/components/common/MbtiAvatar";
import { userKeys, useMyProfileSuspense } from "@/hooks/useUser";
import { useNicknameCheck } from "@/hooks/useNicknameCheck";
import { NICKNAME_REGEX, NICKNAME_MESSAGE } from "@/lib/validationConstants";
import { isAxiosError } from "axios";
import { updateMyProfile } from "@/lib/api/user";
import { FiEdit2, FiMail, FiCalendar, FiUser } from "react-icons/fi";
import { MBTI_VERIFIED_META } from "@/types/user";
import LogoutButton from "@/components/common/LogoutButton";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import NicknameEditDialog from "@/components/my/NicknameEditDialog";
import MbtiEditDialog from "@/components/my/MbtiEditDialog";

const EDITING_FIELD = { NICKNAME: "nickname", MBTI: "mbti" } as const;
type EditingField = null | typeof EDITING_FIELD[keyof typeof EDITING_FIELD];

function MyProfileCardInner() {
  const queryClient = useQueryClient();
  const [editingField, setEditingField] = useState<EditingField>(null);
  const [nickname, setNickname] = useState("");
  const [mbtiId, setMbtiId] = useState("");
  const [nicknameError, setNicknameError] = useState("");

  const { data: profile } = useMyProfileSuspense();
  const { available: nicknameAvailable, taken: nicknameTaken } = useNicknameCheck(nickname, { skipValue: profile.nickname });

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
    if (!NICKNAME_REGEX.test(trimmed)) {
      setNicknameError(NICKNAME_MESSAGE);
      return;
    }
    if (nicknameTaken) {
      setNicknameError("이미 사용 중인 닉네임입니다.");
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
          {/* Mbti Avatar */}
          <div className="-mb-8">
            <MbtiAvatar mbti={getMbtiById(profile.mbtiId)} size="xl" />
          </div>

          {/* Personal Info */}
          <div className="mt-5 w-full space-y-3">

            {/* nickname */}
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiUser className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left flex-1 min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">별명</p>
                <p className="text-sm font-semibold text-foreground truncate">{profile.nickname}</p>
              </div>
              {/* update Btn */}
              <button onClick={openNicknameEdit} className="shrink-0 p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors">
                <FiEdit2 className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* email */}
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiMail className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">이메일</p>
                <p className="text-sm font-semibold text-foreground truncate">{profile.email}</p>
              </div>
            </div>

            {/* Mbti */}
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <span className="text-[11px] font-bold text-muted-foreground shrink-0 w-4 text-center">M</span>
              <div className="text-left flex-1 min-w-0">
                <p className="text-sm font-semibold text-foreground">{profile.mbtiId}</p>
              </div>
              <span style={{ backgroundColor: MBTI_VERIFIED_META[profile.mbtiVerified].color }} className="shrink-0 rounded-md px-2 py-0.5 text-[11px] font-medium text-white">
                {MBTI_VERIFIED_META[profile.mbtiVerified].label}
              </span>
              {/* update Btn */}
              <button onClick={openMbtiEdit} className="shrink-0 p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors">
                <FiEdit2 className="w-3.5 h-3.5" />
              </button>
            </div>

            {/* Created Date */}
            <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/20">
              <FiCalendar className="w-4 h-4 text-muted-foreground shrink-0" />
              <div className="text-left min-w-0">
                <p className="text-[11px] text-muted-foreground uppercase tracking-wider">가입일</p>
                <p className="text-sm text-foreground">{new Date(profile.createdAt).toLocaleDateString("ko-KR")}</p>
              </div>
            </div>

            {/* Logout Btn */}
            <LogoutButton className="w-full flex items-center justify-center gap-1.5 p-2.5 rounded-xl text-sm font-medium text-muted-foreground border border-outline-variant/20 hover:bg-surface-container-low hover:text-foreground transition-colors" />
          </div>
        </div>
      </div>

      <NicknameEditDialog
        open={editingField === EDITING_FIELD.NICKNAME}
        nickname={nickname}
        onNicknameChange={(value) => { setNickname(value); setNicknameError(""); }}
        nicknameError={nicknameError}
        nicknameAvailable={nicknameAvailable}
        nicknameTaken={nicknameTaken}
        isPending={mutation.isPending}
        onSubmit={handleNicknameSave}
        onClose={closeEdit}
      />

      <MbtiEditDialog
        open={editingField === EDITING_FIELD.MBTI}
        mbtiId={mbtiId}
        onMbtiIdChange={setMbtiId}
        currentMbtiId={profile.mbtiId}
        isPending={mutation.isPending}
        isError={mutation.isError}
        onSubmit={handleMbtiSave}
        onClose={closeEdit}
      />
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
