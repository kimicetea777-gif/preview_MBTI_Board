"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import MbtiBadge from "@/components/common/MbtiBadge";
import { fetchMyProfile, updateMyProfile } from "@/lib/api/user";
import { fetchMbtiList } from "@/lib/api/mbti";

export default function MyPageContent() {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [nickname, setNickname] = useState("");
  const [mbtiId, setMbtiId] = useState("");

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
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">마이페이지</h1>

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
    </div>
  );
}
