"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import MbtiBadge from "@/components/common/MbtiBadge";
import { fetchMyProfile, updateMyProfile, sendWithdrawalOtp, withdraw } from "@/lib/api/user";
import { fetchMbtiList } from "@/lib/api/mbti";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { useAuthStore } from "@/store/authStore";
import { ROUTES } from "@/lib/routes";
import MyPostsSection from "@/components/my/MyPostsSection";

type WithdrawalStep = null | "info" | "otp";

export default function MyPageContent() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const setAccessToken = useAuthStore((s) => s.setAccessToken);

  const [isEditing, setIsEditing] = useState(false);
  const [nickname, setNickname] = useState("");
  const [mbtiId, setMbtiId] = useState("");

  const [withdrawalStep, setWithdrawalStep] = useState<WithdrawalStep>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpError, setOtpError] = useState("");

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ["myProfile"],
    queryFn: fetchMyProfile,
  });

  const { data: mbtiList = [] } = useQuery({
    queryKey: ["mbtiList"],
    queryFn: fetchMbtiList,
    staleTime: Infinity,
  });

  const mutation = useMutation({
    mutationFn: updateMyProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(["myProfile"], updated);
      setIsEditing(false);
    },
  });

  const sendOtpMutation = useMutation({
    mutationFn: sendWithdrawalOtp,
    onSuccess: () => {
      setWithdrawalStep("otp");
      setOtpCode("");
      setOtpError("");
    },
    onError: () => {
      toast.error("인증 메일 발송에 실패했습니다. 다시 시도해 주세요.");
    },
  });

  const withdrawMutation = useMutation({
    mutationFn: () => withdraw(otpCode),
    onSuccess: () => {
      setWithdrawalStep(null);
      setAccessToken(null);
      queryClient.clear();
      toast.success("탈퇴되었습니다.");
      router.replace("/");
    },
    onError: (error: unknown) => {
      const axiosError = error as { response?: { data?: { message?: string } } };
      const message =
        axiosError?.response?.data?.message ??
        "인증 코드가 올바르지 않거나 만료되었습니다.";
      setOtpError(message);
    },
  });

  const handleWithdrawalClose = () => {
    if (sendOtpMutation.isPending || withdrawMutation.isPending) return;
    setWithdrawalStep(null);
    setOtpCode("");
    setOtpError("");
    sendOtpMutation.reset();
    withdrawMutation.reset();
  };

  const handleEditStart = () => {
    if (profile) {
      setNickname(profile.nickname);
      setMbtiId(profile.mbtiId);
    }
    setIsEditing(true);
  };

  const handleCancel = () => {
    setIsEditing(false);
    mutation.reset();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;

    const changes: { nickname?: string; mbtiId?: string } = {};
    if (nickname.trim() !== profile.nickname) changes.nickname = nickname.trim();
    if (mbtiId !== profile.mbtiId) changes.mbtiId = mbtiId;

    if (Object.keys(changes).length === 0) {
      setIsEditing(false);
      return;
    }
    mutation.mutate(changes);
  };

  if (isLoading) {
    return <div className="py-10 text-center text-gray-500">불러오는 중...</div>;
  }

  if (isError || !profile) {
    return <div className="py-10 text-center text-red-500">프로필을 불러올 수 없습니다.</div>;
  }

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">마이페이지</h1>

      {/* 프로필 카드 */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <div>
          <span className="text-xs text-gray-400 uppercase tracking-wide">이메일</span>
          <p className="mt-1 text-sm text-gray-700">{profile.email}</p>
        </div>

        {!isEditing ? (
          <>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">닉네임</span>
              <p className="mt-1 text-sm font-medium text-gray-900">{profile.nickname}</p>
            </div>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">MBTI</span>
              <div className="mt-1">
                <MbtiBadge mbti={profile.mbtiId} verified={profile.mbtiVerified} />
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">가입일</span>
              <p className="mt-1 text-sm text-gray-500">
                {new Date(profile.createdAt).toLocaleDateString("ko-KR")}
              </p>
            </div>
            <button
              onClick={handleEditStart}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
            >
              프로필 수정
            </button>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                닉네임
              </label>
              <input
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                minLength={2}
                maxLength={50}
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                MBTI
              </label>
              <select
                value={mbtiId}
                onChange={(e) => setMbtiId(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none"
              >
                {mbtiList.map((mbti) => (
                  <option key={mbti.id} value={mbti.id}>
                    {mbti.id === "UNKNOWN" ? "미설정" : mbti.id}
                  </option>
                ))}
              </select>
              {mbtiId !== profile.mbtiId && (
                <p className="mt-1 text-xs text-amber-600">
                  MBTI 변경 시 인증 상태가 &apos;자기입력&apos;으로 초기화됩니다.
                </p>
              )}
            </div>
            {mutation.isError && (
              <p className="text-xs text-red-500">수정에 실패했습니다. 다시 시도해 주세요.</p>
            )}
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={handleCancel}
                disabled={mutation.isPending}
                className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
              >
                취소
              </button>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="flex-1 rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
              >
                {mutation.isPending ? "저장 중..." : "저장"}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* 내가 작성한 게시글 */}
      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">내가 작성한 게시글</h2>
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <MyPostsSection />
        </div>
      </section>

      {/* 위험 구역 */}
      <section>
        <h2 className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-2">위험 구역</h2>
        <div className="rounded-xl border border-red-100 bg-red-50/40 p-4 flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-gray-800">회원 탈퇴</p>
            <p className="text-xs text-gray-500 mt-0.5">탈퇴 시 개인 정보가 삭제되고 복구할 수 없습니다.</p>
          </div>
          <button
            onClick={() => setWithdrawalStep("info")}
            className="shrink-0 rounded-lg border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors"
          >
            탈퇴하기
          </button>
        </div>
      </section>

      {/* 탈퇴 안내 모달 (Step 1) */}
      <Dialog
        open={withdrawalStep === "info"}
        onOpenChange={(open) => !open && handleWithdrawalClose()}
      >
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>회원 탈퇴</DialogTitle>
            <DialogDescription>탈퇴 전 다음 사항을 확인해 주세요.</DialogDescription>
          </DialogHeader>
          <ul className="text-sm text-gray-600 space-y-1.5 list-disc list-inside">
            <li>탈퇴 시 모든 개인 정보가 삭제됩니다.</li>
            <li>작성하신 게시글과 댓글은 익명으로 유지됩니다.</li>
            <li>탈퇴 후 동일 이메일로 재가입이 가능합니다.</li>
          </ul>
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={handleWithdrawalClose}
              disabled={sendOtpMutation.isPending}
              className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={() => sendOtpMutation.mutate()}
              disabled={sendOtpMutation.isPending}
              className="flex-1 rounded-lg bg-red-600 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              {sendOtpMutation.isPending ? "발송 중..." : "인증 메일 받기"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* OTP 입력 모달 (Step 2) */}
      <Dialog
        open={withdrawalStep === "otp"}
        onOpenChange={(open) => !open && handleWithdrawalClose()}
      >
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>탈퇴 인증</DialogTitle>
            <DialogDescription>
              가입한 이메일로 인증 코드가 발송되었습니다.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <input
              type="text"
              value={otpCode}
              onChange={(e) => {
                setOtpCode(e.target.value.replace(/\D/g, "").slice(0, 6));
                setOtpError("");
              }}
              placeholder="6자리 인증 코드"
              maxLength={6}
              inputMode="numeric"
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-center tracking-widest focus:border-gray-500 focus:outline-none"
            />
            {otpError && <p className="text-xs text-red-500">{otpError}</p>}
            <button
              type="button"
              onClick={() => sendOtpMutation.mutate()}
              disabled={sendOtpMutation.isPending}
              className="text-xs text-gray-400 hover:text-gray-600 underline disabled:no-underline disabled:opacity-50"
            >
              {sendOtpMutation.isPending ? "발송 중..." : "인증 메일 재발송"}
            </button>
          </div>
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={handleWithdrawalClose}
              disabled={withdrawMutation.isPending}
              className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="button"
              onClick={() => withdrawMutation.mutate()}
              disabled={withdrawMutation.isPending || otpCode.length < 6}
              className="flex-1 rounded-lg bg-red-600 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              {withdrawMutation.isPending ? "처리 중..." : "탈퇴 확인"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
