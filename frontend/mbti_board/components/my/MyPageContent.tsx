"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { toast } from "@/components/common/Toast";
import MbtiBadge from "@/components/common/MbtiBadge";
import MbtiSelect from "@/components/common/MbtiSelect";
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
      toast.error("?몄쬆 硫붿씪 諛쒖넚???ㅽ뙣?덉뒿?덈떎. ?ㅼ떆 ?쒕룄??二쇱꽭??");
    },
  });

  const withdrawMutation = useMutation({
    mutationFn: () => withdraw(otpCode),
    onSuccess: () => {
      setWithdrawalStep(null);
      setAccessToken(null);
      queryClient.clear();
      toast.success("?덊눜?섏뿀?듬땲??");
      router.replace("/");
    },
    onError: (error: unknown) => {
      const axiosError = error as { response?: { data?: { message?: string } } };
      const message =
        axiosError?.response?.data?.message ??
        "?몄쬆 肄붾뱶媛 ?щ컮瑜댁? ?딄굅??留뚮즺?섏뿀?듬땲??";
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
    return <div className="py-10 text-center text-gray-500">遺덈윭?ㅻ뒗 以?..</div>;
  }

  if (isError || !profile) {
    return <div className="py-10 text-center text-red-500">?꾨줈?꾩쓣 遺덈윭?????놁뒿?덈떎.</div>;
  }

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">留덉씠?섏씠吏</h1>

      {/* ?꾨줈??移대뱶 */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <div>
          <span className="text-xs text-gray-400 uppercase tracking-wide">?대찓??/span>
          <p className="mt-1 text-sm text-gray-700">{profile.email}</p>
        </div>

        {!isEditing ? (
          <>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">?됰꽕??/span>
              <p className="mt-1 text-sm font-medium text-gray-900">{profile.nickname}</p>
            </div>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">MBTI</span>
              <div className="mt-1">
                <MbtiBadge mbti={profile.mbtiId} verified={profile.mbtiVerified} />
              </div>
            </div>
            <div>
              <span className="text-xs text-gray-400 uppercase tracking-wide">媛?낆씪</span>
              <p className="mt-1 text-sm text-gray-500">
                {new Date(profile.createdAt).toLocaleDateString("ko-KR")}
              </p>
            </div>
            <button
              onClick={handleEditStart}
              className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
            >
              ?꾨줈???섏젙
            </button>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                ?됰꽕??              </label>
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
              <MbtiSelect
                value={mbtiId}
                onChange={setMbtiId}
                mbtiList={mbtiList}
              />
              {mbtiId !== profile.mbtiId && (
                <p className="mt-1 text-xs text-amber-600">
                  MBTI 蹂寃????몄쬆 ?곹깭媛 &apos;?먭린?낅젰&apos;?쇰줈 珥덇린?붾맗?덈떎.
                </p>
              )}
            </div>
            {mutation.isError && (
              <p className="text-xs text-red-500">?섏젙???ㅽ뙣?덉뒿?덈떎. ?ㅼ떆 ?쒕룄??二쇱꽭??</p>
            )}
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={handleCancel}
                disabled={mutation.isPending}
                className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
              >
                痍⑥냼
              </button>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="flex-1 rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
              >
                {mutation.isPending ? "???以?.." : "???}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* ?닿? ?묒꽦??寃뚯떆湲 */}
      <section>
        <h2 className="text-lg font-semibold text-gray-900 mb-3">?닿? ?묒꽦??寃뚯떆湲</h2>
        <div className="bg-white rounded-xl border border-gray-200 p-4">
          <MyPostsSection />
        </div>
      </section>

      {/* ?꾪뿕 援ъ뿭 */}
      <section>
        <h2 className="text-xs font-medium text-gray-400 uppercase tracking-wide mb-2">?꾪뿕 援ъ뿭</h2>
        <div className="rounded-xl border border-red-100 bg-red-50/40 p-4 flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-gray-800">?뚯썝 ?덊눜</p>
            <p className="text-xs text-gray-500 mt-0.5">?덊눜 ??媛쒖씤 ?뺣낫媛 ??젣?섍퀬 蹂듦뎄?????놁뒿?덈떎.</p>
          </div>
          <button
            onClick={() => setWithdrawalStep("info")}
            className="shrink-0 rounded-lg border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 transition-colors"
          >
            ?덊눜?섍린
          </button>
        </div>
      </section>

      {/* ?덊눜 ?덈궡 紐⑤떖 (Step 1) */}
      <Dialog
        open={withdrawalStep === "info"}
        onOpenChange={(open) => !open && handleWithdrawalClose()}
      >
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>?뚯썝 ?덊눜</DialogTitle>
            <DialogDescription>?덊눜 ???ㅼ쓬 ?ы빆???뺤씤??二쇱꽭??</DialogDescription>
          </DialogHeader>
          <ul className="text-sm text-gray-600 space-y-1.5 list-disc list-inside">
            <li>?덊눜 ??紐⑤뱺 媛쒖씤 ?뺣낫媛 ??젣?⑸땲??</li>
            <li>?묒꽦?섏떊 寃뚯떆湲怨??볤?? ?듬챸?쇰줈 ?좎??⑸땲??</li>
            <li>?덊눜 ???숈씪 ?대찓?쇰줈 ?ш??낆씠 媛?ν빀?덈떎.</li>
          </ul>
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={handleWithdrawalClose}
              disabled={sendOtpMutation.isPending}
              className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
            >
              痍⑥냼
            </button>
            <button
              type="button"
              onClick={() => sendOtpMutation.mutate()}
              disabled={sendOtpMutation.isPending}
              className="flex-1 rounded-lg bg-red-600 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              {sendOtpMutation.isPending ? "諛쒖넚 以?.." : "?몄쬆 硫붿씪 諛쏄린"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* OTP ?낅젰 紐⑤떖 (Step 2) */}
      <Dialog
        open={withdrawalStep === "otp"}
        onOpenChange={(open) => !open && handleWithdrawalClose()}
      >
        <DialogContent showCloseButton={false}>
          <DialogHeader>
            <DialogTitle>?덊눜 ?몄쬆</DialogTitle>
            <DialogDescription>
              媛?낇븳 ?대찓?쇰줈 ?몄쬆 肄붾뱶媛 諛쒖넚?섏뿀?듬땲??
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
              placeholder="6?먮━ ?몄쬆 肄붾뱶"
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
              {sendOtpMutation.isPending ? "諛쒖넚 以?.." : "?몄쬆 硫붿씪 ?щ컻??}
            </button>
          </div>
          <DialogFooter className="flex-row gap-2">
            <button
              type="button"
              onClick={handleWithdrawalClose}
              disabled={withdrawMutation.isPending}
              className="flex-1 rounded-lg border border-gray-300 py-2 text-sm hover:bg-gray-50 disabled:opacity-50"
            >
              痍⑥냼
            </button>
            <button
              type="button"
              onClick={() => withdrawMutation.mutate()}
              disabled={withdrawMutation.isPending || otpCode.length < 6}
              className="flex-1 rounded-lg bg-red-600 py-2 text-sm text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              {withdrawMutation.isPending ? "泥섎━ 以?.." : "?덊눜 ?뺤씤"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

