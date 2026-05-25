"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { createPost } from "@/lib/api/post";
import { fetchMbtiList } from "@/lib/api/mbti";
import { postKeys } from "@/hooks/usePosts";
import { ROUTES } from "@/lib/routes";
import PostWriteForm from "@/components/post/PostWriteForm";
import { toast } from "sonner";
import { isTiptapEmpty } from "@/lib/editor";

export default function FreeBoardWritePage() {
  const router = useRouter();
  const accessToken = useAuthStore((s) => s.accessToken);
  const showGuestField = accessToken === null;

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState("ALL");
  const [guestPassword, setGuestPassword] = useState("");

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
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error?.response?.data?.message ?? "오류가 발생했습니다.");
    },
  });

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!title.trim()) {
      toast.error("제목을 입력해 주세요.");
      return;
    }
    if (title.trim().length > 150) {
      toast.error("제목은 150자 이내로 입력해 주세요.");
      return;
    }
    if (isTiptapEmpty(content)) {
      toast.error("내용을 입력해 주세요.");
      return;
    }
    if (showGuestField && (guestPassword.length < 4 || guestPassword.length > 72)) {
      toast.error("비밀번호는 4~72자로 입력해 주세요.");
      return;
    }

    mutation.mutate({
      type: "FREE",
      title: title.trim(),
      content,
      targetMbti: targetMbti === "ALL" ? undefined : targetMbti,
      guestPassword: showGuestField ? guestPassword : undefined,
    });
  };

  return (
    <PostWriteForm
      heading="글쓰기"
      isPending={mutation.isPending}
      onSubmit={handleSubmit}
      title={title}
      onTitleChange={setTitle}
      targetMbti={targetMbti}
      onTargetMbtiChange={setTargetMbti}
      onContentChange={setContent}
      mbtiList={mbtiList}
    >
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
    </PostWriteForm>
  );
}
