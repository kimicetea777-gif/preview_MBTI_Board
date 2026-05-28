"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { createPost, createVotePost, createDebatePost } from "@/lib/api/post";
import { postKeys } from "@/hooks/usePosts";
import { useMbtiList } from "@/hooks/useMbti";
import { ROUTES } from "@/lib/routes";
import PostWriteForm from "@/components/post/PostWriteForm";
import { toast } from "sonner";
import { isTiptapEmpty, EMPTY_TIPTAP } from "@/lib/editor";
import { MBTI_ALL, NONE_MBTI_ID } from "@/types/mbti";
import { VOTE_OPTION_MIN, VOTE_OPTION_MAX, POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType, VoteOptionRequest } from "@/types/post";
import { getMinDatetimeLocal } from "@/lib/dateUtils";

interface OptionRow {
  optionText: string;
  mbtiTarget: string;
}

interface Props {
  initialType: PostType;
}

export default function WriteFormContent({ initialType }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isLoggedIn = useAuthStore((s) => s.accessToken) !== null;

  const [postType, setPostType] = useState<PostType>(initialType);

  // 공통
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState(MBTI_ALL);

  // FREE 전용
  const [guestPassword, setGuestPassword] = useState("");

  // VOTE 전용
  const [isMbtiMode, setIsMbtiMode] = useState(false);
  const [options, setOptions] = useState<OptionRow[]>([
    { optionText: "", mbtiTarget: "" },
    { optionText: "", mbtiTarget: "" },
  ]);

  // VOTE + DEBATE 공통
  const [endsAt, setEndsAt] = useState("");
  const [isNoDeadline, setIsNoDeadline] = useState(false);

  // DEBATE 전용
  const [proLabel, setProLabel] = useState("");
  const [conLabel, setConLabel] = useState("");

  const { data: mbtiList = [] } = useMbtiList();


  /* -------------------------------------API---------------------------------------- */
  const onError = (err: { response?: { data?: { message?: string } } }) =>
  toast.error(err?.response?.data?.message ?? "오류가 발생했습니다.");

  const freeMutation = useMutation({
    mutationFn: createPost,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.freePost(data.id));
    },
    onError,
  });

  const voteMutation = useMutation({
    mutationFn: createVotePost,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.votePost(data.id));
    },
    onError,
  });

  const debateMutation = useMutation({
    mutationFn: createDebatePost,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.debatePost(data.id));
    },
    onError,
  });

  const isPending = freeMutation.isPending || voteMutation.isPending || debateMutation.isPending;

  const handleTypeChange = (type: PostType) => {
    if (type !== POST_TYPE.FREE && !isLoggedIn) {
      toast.error("로그인 후 이용할 수 있습니다.");
      return;
    }
    setPostType(type);
  };

  
  /* ------------------------------------- VOTE 선택지 핸들러 ---------------------------------------- */
  const toggleMbtiMode = () => {
    if (!isMbtiMode) {
      const mbtiOptions = mbtiList
        .filter((m) => m.id !== NONE_MBTI_ID)
        .map((m) => ({ optionText: m.id, mbtiTarget: m.id }));
      setOptions(
        mbtiOptions.length >= VOTE_OPTION_MIN ? mbtiOptions : options
      );
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

  const validateDeadline = (): boolean => {
    if (!isNoDeadline && !!endsAt && new Date(endsAt) <= new Date()) {
      toast.error("마감일은 현재 시각 이후여야 합니다.");
      return false;
    }
    return true;
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!title.trim()) { toast.error("제목을 입력해 주세요."); return; }
    if (title.trim().length > 150) { toast.error("제목은 150자 이내로 입력해 주세요."); return; }

    if (postType === POST_TYPE.FREE) {
      if (isTiptapEmpty(content)) { toast.error("내용을 입력해 주세요."); return; }
      if (!isLoggedIn && (guestPassword.length < 4 || guestPassword.length > 72)) {
        toast.error("비밀번호는 4~72자로 입력해 주세요."); return;
      }
      freeMutation.mutate({
        type: POST_TYPE.FREE,
        title: title.trim(),
        content,
        targetMbti: targetMbti === MBTI_ALL ? undefined : targetMbti,
        guestPassword: !isLoggedIn ? guestPassword : undefined,
      });
      return;
    }

    const resolvedEndsAt = !isNoDeadline && endsAt ? new Date(endsAt).toISOString() : undefined;

    if (postType === POST_TYPE.VOTE) {
      const filled = isMbtiMode
        ? options
        : options.filter((o) => o.optionText.trim());
      if (filled.length < VOTE_OPTION_MIN) {
        toast.error(`선택지를 최소 ${VOTE_OPTION_MIN}개 이상 입력해 주세요.`); return;
      }
      if (!validateDeadline()) return;
      const voteOptions: VoteOptionRequest[] = filled.map((o) => ({
        optionText: isMbtiMode ? o.optionText : o.optionText.trim(),
        mbtiTarget: isMbtiMode ? o.mbtiTarget : undefined,
      }));
      voteMutation.mutate({
        title: title.trim(),
        content: content || EMPTY_TIPTAP,
        targetMbti: targetMbti === MBTI_ALL ? undefined : targetMbti,
        voteOptions,
        endsAt: resolvedEndsAt,
      });
      return;
    }

    // DEBATE
    if (!validateDeadline()) return;
    debateMutation.mutate({
      title: title.trim(),
      content: content || EMPTY_TIPTAP,
      targetMbti: targetMbti === MBTI_ALL ? undefined : targetMbti,
      proLabel: proLabel.trim() || undefined,
      conLabel: conLabel.trim() || undefined,
      endsAt: resolvedEndsAt,
    });
  };

  const typeToggle = (
    <div className="flex gap-1 border border-gray-200 rounded-lg p-1 w-fit">
      {(Object.values(POST_TYPE) as PostType[]).map((type) => (
        <button
          key={type}
          type="button"
          onClick={() => handleTypeChange(type)}
          disabled={isPending}
          className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors disabled:opacity-50 ${
            postType === type
              ? "bg-gray-900 text-white"
              : "text-gray-500 hover:text-gray-900"
          }`}
        >
          {POST_TYPE_LABELS[type]}
        </button>
      ))}
    </div>
  );

  const deadlineField = (
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
          onClick={() => { setIsNoDeadline((prev) => !prev); setEndsAt(""); }}
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
  );

  return (
    <PostWriteForm
      heading="글쓰기"
      isPending={isPending}
      onSubmit={handleSubmit}
      title={title}
      onTitleChange={setTitle}
      targetMbti={targetMbti}
      onTargetMbtiChange={setTargetMbti}
      onContentChange={setContent}
      mbtiList={mbtiList}
      beforeTitle={typeToggle}
      contentLabel={postType !== POST_TYPE.FREE ? "내용 (선택)" : undefined}
      contentPlaceholder={
        postType === POST_TYPE.VOTE ? "투표에 대한 설명을 입력하세요."
          : postType === POST_TYPE.DEBATE ? "토론 주제에 대한 설명을 입력하세요."
          : "내용을 입력하세요."
      }
    >
      {/* FREE: 비회원 비밀번호 */}
      {postType === POST_TYPE.FREE && !isLoggedIn && (
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
            disabled={isPending}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
          />
        </div>
      )}

      {/* VOTE: 선택지 + 마감일 */}
      {postType === POST_TYPE.VOTE && (
        <>
          <fieldset className="border border-gray-200 rounded-lg p-4 space-y-3">
            <legend className="px-1 text-sm font-medium text-gray-700">투표 선택지</legend>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={toggleMbtiMode}
                disabled={
                  isPending ||
                  mbtiList.filter((m) => m.id !== NONE_MBTI_ID).length < VOTE_OPTION_MIN
                }
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

          {deadlineField}
        </>
      )}

      {/* DEBATE: 찬반 레이블 + 마감일 */}
      {postType === POST_TYPE.DEBATE && (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                찬성 레이블 (선택)
              </label>
              <input
                type="text"
                value={proLabel}
                onChange={(e) => setProLabel(e.target.value)}
                placeholder="찬성"
                maxLength={50}
                disabled={isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                반대 레이블 (선택)
              </label>
              <input
                type="text"
                value={conLabel}
                onChange={(e) => setConLabel(e.target.value)}
                placeholder="반대"
                maxLength={50}
                disabled={isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
          </div>

          {deadlineField}
        </>
      )}
    </PostWriteForm>
  );
}
