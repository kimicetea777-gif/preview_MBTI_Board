"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createPost } from "@/lib/api/post";
import { fetchMbtiList } from "@/lib/api/mbti";
import { postKeys } from "@/hooks/usePosts";
import { ROUTES } from "@/lib/routes";
import { VOTE_OPTION_MIN, VOTE_OPTION_MAX } from "@/types/post";
import type { VoteOptionRequest } from "@/types/post";
import PostWriteForm from "@/components/post/PostWriteForm";
import { toast } from "sonner";
import { NONE_MBTI_ID } from "@/types/mbti";
import { EMPTY_TIPTAP } from "@/lib/editor";
import { getMinDatetimeLocal } from "@/lib/dateUtils";

interface OptionRow {
  optionText: string;
  mbtiTarget: string;
}

export default function VoteBoardWritePage() {
  const router = useRouter();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState("ALL");

  const [isMbtiMode, setIsMbtiMode] = useState(false);
  const [options, setOptions] = useState<OptionRow[]>([
    { optionText: "", mbtiTarget: "" },
    { optionText: "", mbtiTarget: "" },
  ]);
  const [endsAt, setEndsAt] = useState("");
  const [isNoDeadline, setIsNoDeadline] = useState(false);

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
      router.push(ROUTES.board.votePost(data.id));
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error?.response?.data?.message ?? "오류가 발생했습니다.");
    },
  });

  const toggleMbtiMode = () => {
    if (!isMbtiMode) {
      const mbtiOptions = mbtiList
        .filter((m) => m.id !== NONE_MBTI_ID)
        .map((m) => ({ optionText: m.id, mbtiTarget: m.id }));
      setOptions(mbtiOptions.length >= VOTE_OPTION_MIN ? mbtiOptions : options);
    } else {
      setOptions([
        { optionText: "", mbtiTarget: "" },
        { optionText: "", mbtiTarget: "" },
      ]);
    }
    setIsMbtiMode((prev) => !prev);
  };

  const addOption = () => {
    if (options.length >= VOTE_OPTION_MAX) return;
    setOptions((prev) => [...prev, { optionText: "", mbtiTarget: "" }]);
  };

  const removeOption = (index: number) => {
    if (options.length <= VOTE_OPTION_MIN) return;
    setOptions((prev) => prev.filter((_, i) => i !== index));
  };

  const updateOptionText = (index: number, value: string) => {
    setOptions((prev) =>
      prev.map((o, i) => (i === index ? { ...o, optionText: value } : o))
    );
  };

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

    const filledOptions = isMbtiMode
      ? options
      : options.filter((o) => o.optionText.trim());

    if (filledOptions.length < VOTE_OPTION_MIN) {
      toast.error(`선택지를 최소 ${VOTE_OPTION_MIN}개 이상 입력해 주세요.`);
      return;
    }

    if (!isNoDeadline && endsAt && new Date(endsAt) <= new Date()) {
      toast.error("마감일은 현재 시각 이후여야 합니다.");
      return;
    }

    const voteOptions: VoteOptionRequest[] = filledOptions.map((o) => ({
      optionText: isMbtiMode ? o.optionText : o.optionText.trim(),
      mbtiTarget: isMbtiMode ? o.mbtiTarget : undefined,
    }));

    mutation.mutate({
      type: "VOTE",
      title: title.trim(),
      content: content || EMPTY_TIPTAP,
      targetMbti: targetMbti === "ALL" ? undefined : targetMbti,
      voteOptions,
      endsAt: (!isNoDeadline && endsAt) ? new Date(endsAt).toISOString() : undefined,
    });
  };

  const isPending = mutation.isPending;

  return (
    <PostWriteForm
      heading="투표 게시글 작성"
      isPending={isPending}
      onSubmit={handleSubmit}
      title={title}
      onTitleChange={setTitle}
      targetMbti={targetMbti}
      onTargetMbtiChange={setTargetMbti}
      onContentChange={setContent}
      mbtiList={mbtiList}
      contentLabel="내용 (선택)"
      contentPlaceholder="투표에 대한 설명을 입력하세요."
    >
      <fieldset className="border border-gray-200 rounded-lg p-4 space-y-3">
        <legend className="px-1 text-sm font-medium text-gray-700">투표 선택지</legend>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={toggleMbtiMode}
            disabled={isPending || mbtiList.filter((m) => m.id !== NONE_MBTI_ID).length < VOTE_OPTION_MIN}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors disabled:opacity-50 ${
              isMbtiMode
                ? "bg-gray-900 text-white border-gray-900"
                : "bg-white text-gray-600 border-gray-300 hover:border-gray-500"
            }`}
          >
            MBTI 투표 모드 {isMbtiMode ? "ON" : "OFF"}
          </button>
          {isMbtiMode && (
            <span className="text-xs text-gray-400">
              MBTI 유형별 선택지가 자동 생성됩니다.
            </span>
          )}
        </div>

        <ul className="space-y-2">
          {options.map((option, index) => (
            <li key={index} className="flex items-center gap-2">
              <span className="text-xs text-gray-400 w-5 shrink-0">{index + 1}.</span>
              {isMbtiMode ? (
                <span className="flex-1 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700">
                  {option.optionText}
                </span>
              ) : (
                <input
                  type="text"
                  value={option.optionText}
                  onChange={(e) => updateOptionText(index, e.target.value)}
                  maxLength={100}
                  placeholder={`선택지 ${index + 1}`}
                  disabled={isPending}
                  className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
                />
              )}
              <button
                type="button"
                onClick={() => removeOption(index)}
                disabled={isPending || options.length <= VOTE_OPTION_MIN}
                className="text-gray-400 hover:text-red-500 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-lg leading-none"
                aria-label="선택지 삭제"
              >
                ×
              </button>
            </li>
          ))}
        </ul>

        {!isMbtiMode && (
          <button
            type="button"
            onClick={addOption}
            disabled={isPending || options.length >= VOTE_OPTION_MAX}
            className="text-sm text-gray-500 hover:text-gray-900 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
          >
            + 선택지 추가 ({options.length}/{VOTE_OPTION_MAX})
          </button>
        )}
      </fieldset>

      <div>
        <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
          마감일
        </label>
        <div className="flex items-center gap-2">
          <input
            type="datetime-local"
            value={endsAt}
            onChange={(e) => setEndsAt(e.target.value)}
            min={getMinDatetimeLocal()}
            disabled={isPending || isNoDeadline}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50 disabled:bg-gray-50"
          />
          <button
            type="button"
            onClick={() => {
              setIsNoDeadline((prev) => !prev);
              setEndsAt("");
            }}
            disabled={isPending}
            className={`px-3 py-2 rounded-lg text-sm font-medium border transition-colors disabled:opacity-50 ${
              isNoDeadline
                ? "bg-gray-900 text-white border-gray-900"
                : "bg-white text-gray-600 border-gray-300 hover:border-gray-500"
            }`}
          >
            기한 없음
          </button>
        </div>
      </div>
    </PostWriteForm>
  );
}
