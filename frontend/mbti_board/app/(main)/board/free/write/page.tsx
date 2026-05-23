"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { createPost } from "@/lib/api/post";
import { fetchMbtiList } from "@/lib/api/mbti";
import { postKeys } from "@/hooks/usePosts";
import RichContentEditor from "@/components/post/RichContentEditor";
import { ROUTES } from "@/lib/routes";

function isTiptapEmpty(json: string): boolean {
  if (!json) return true;
  try {
    const doc = JSON.parse(json);
    const extractText = (node: { type: string; text?: string; content?: unknown[] }): string => {
      if (node.type === "text") return node.text ?? "";
      return (node.content ?? []).map((n) => extractText(n as typeof node)).join("");
    };
    return !extractText(doc).trim();
  } catch {
    return true;
  }
}

export default function FreeBoardWritePage() {
  const router = useRouter();
  const accessToken = useAuthStore((s) => s.accessToken);
  const showGuestField = accessToken === null;

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState("ALL");
  const [guestPassword, setGuestPassword] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  const { data: mbtiList = [] } = useQuery({
    queryKey: ["mbtiList"],
    queryFn: fetchMbtiList,
    staleTime: Infinity,
  });

  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: createPost,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.freePost(data.id));
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setValidationError("제목을 입력해 주세요.");
      return;
    }
    if (title.trim().length > 150) {
      setValidationError("제목은 150자 이내로 입력해 주세요.");
      return;
    }
    if (isTiptapEmpty(content)) {
      setValidationError("내용을 입력해 주세요.");
      return;
    }
    if (showGuestField && (guestPassword.length < 4 || guestPassword.length > 72)) {
      setValidationError("비밀번호는 4~72자로 입력해 주세요.");
      return;
    }

    setValidationError(null);
    mutation.mutate({
      type: "FREE",
      title: title.trim(),
      content,
      targetMbti: targetMbti === "ALL" ? undefined : targetMbti,
      guestPassword: showGuestField ? guestPassword : undefined,
    });
  };

  const serverError = mutation.isError
    ? ((mutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? "오류가 발생했습니다.")
    : null;

  return (
    <section className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-xl font-bold text-gray-900 mb-6">글쓰기</h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={150}
          placeholder="제목"
          disabled={mutation.isPending}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
        />

        <div>
          <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
            대상 MBTI
          </label>
          <select
            value={targetMbti}
            onChange={(e) => setTargetMbti(e.target.value)}
            disabled={mutation.isPending}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
          >
            <option value="ALL">전체</option>
            {mbtiList.map((m) => (
              <option key={m.id} value={m.id}>{m.id}</option>
            ))}
          </select>
        </div>

        <RichContentEditor onChange={setContent} placeholder="내용을 입력하세요." />

        {showGuestField && (
          <div>
            <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
              비회원 비밀번호
            </label>
            <input
              type="password"
              value={guestPassword}
              onChange={(e) => setGuestPassword(e.target.value)}
              placeholder="비회원 비밀번호 (4~72자)"
              minLength={4}
              maxLength={72}
              autoComplete="off"
              disabled={mutation.isPending}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
            />
          </div>
        )}

        {(validationError ?? serverError) && (
          <p className="text-xs text-red-500">{validationError ?? serverError}</p>
        )}

        <div className="flex gap-2 justify-end">
          <button
            type="button"
            onClick={() => router.back()}
            disabled={mutation.isPending}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
          >
            {mutation.isPending ? "등록 중..." : "등록"}
          </button>
        </div>
      </form>
    </section>
  );
}
