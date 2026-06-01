"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { createPost, createVotePost, createDebatePost } from "@/lib/api/post";
import { postKeys } from "@/hooks/usePosts";
import { useMbtiList } from "@/hooks/useMbti";
import { ROUTES } from "@/lib/routes";
import PostWriteForm from "@/components/post/PostWriteForm";
import { toast } from "@/components/common/Toast";
import { isTiptapEmpty, EMPTY_TIPTAP } from "@/lib/editor";
import { MBTI_ALL, NONE_MBTI_ID } from "@/types/mbti";
import { VOTE_OPTION_MIN, VOTE_OPTION_MAX, POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType, VoteOptionRequest } from "@/types/post";
import { getMinDatetimeLocal } from "@/lib/dateUtils";
import { API } from "@/lib/apiEndpoints";

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

  // ?낅줈?쒕맂 ?대?吏 URL 異붿쟻 ??寃뚯떆湲 ????놁씠 ?댄깉 ??PENDING 痍⑥냼 ?붿껌???ъ슜
  const uploadedUrlsRef = useRef<Set<string>>(new Set());
  // ????깃났 ??true濡??ㅼ젙??unmount cleanup??痍⑥냼 ?붿껌??蹂대궡吏 ?딅룄濡?諛⑹뼱
  const savedRef = useRef(false);

  const handleUpload = useCallback((url: string) => {
    uploadedUrlsRef.current.add(url);
  }, []);

  // 而댄룷?뚰듃 ?몃쭏?댄듃(痍⑥냼쨌?ㅻ줈媛湲걔룻꺆 ?댄깉) ??PENDING ?대?吏 痍⑥냼 ?붿껌
  useEffect(() => {
    // ref 媛앹껜 ?먯껜瑜?罹≪쿂 (cleanup ?쒖젏??.current 理쒖떊媛믪쓣 ?쎄린 ?꾪븿)
    const urlsRef = uploadedUrlsRef;
    const isSavedRef = savedRef;
    return () => {
      if (isSavedRef.current || urlsRef.current.size === 0) return;
      const urls = [...urlsRef.current];
      // keepalive: ?섏씠吏 ?몃줈??以묒뿉???붿껌???꾨즺?????덈룄濡?蹂댁옣
      fetch(API.images.cancel, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ urls }),
        keepalive: true,
        credentials: "include",
      });
    };
  }, []);

  const [postType, setPostType] = useState<PostType>(initialType);

  // 怨듯넻
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState(MBTI_ALL);

  // FREE ?꾩슜
  const [guestPassword, setGuestPassword] = useState("");

  // VOTE ?꾩슜
  const [isMbtiMode, setIsMbtiMode] = useState(false);
  const [options, setOptions] = useState<OptionRow[]>([
    { optionText: "", mbtiTarget: "" },
    { optionText: "", mbtiTarget: "" },
  ]);

  // VOTE + DEBATE 怨듯넻
  const [endsAt, setEndsAt] = useState("");
  const [isNoDeadline, setIsNoDeadline] = useState(false);

  // DEBATE ?꾩슜
  const [proLabel, setProLabel] = useState("");
  const [conLabel, setConLabel] = useState("");

  const { data: mbtiList = [] } = useMbtiList();


  /* -------------------------------------API---------------------------------------- */
  const onError = (err: { response?: { data?: { message?: string } } }) =>
  toast.error(err?.response?.data?.message ?? "?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.");

  const freeMutation = useMutation({
    mutationFn: createPost,
    onSuccess: (data) => {
      savedRef.current = true;
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.freePost(data.id));
    },
    onError,
  });

  const voteMutation = useMutation({
    mutationFn: createVotePost,
    onSuccess: (data) => {
      savedRef.current = true;
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.votePost(data.id));
    },
    onError,
  });

  const debateMutation = useMutation({
    mutationFn: createDebatePost,
    onSuccess: (data) => {
      savedRef.current = true;
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.debatePost(data.id));
    },
    onError,
  });

  const isPending = freeMutation.isPending || voteMutation.isPending || debateMutation.isPending;

  const handleTypeChange = (type: PostType) => {
    if (type !== POST_TYPE.FREE && !isLoggedIn) {
      toast.error("濡쒓렇?????댁슜?????덉뒿?덈떎.");
      return;
    }
    setPostType(type);
  };

  
  /* ------------------------------------- VOTE ?좏깮吏 ?몃뱾??---------------------------------------- */
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
      toast.error("留덇컧?쇱? ?꾩옱 ?쒓컖 ?댄썑?ъ빞 ?⑸땲??");
      return false;
    }
    return true;
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!title.trim()) { toast.error("?쒕ぉ???낅젰??二쇱꽭??"); return; }
    if (title.trim().length > 150) { toast.error("?쒕ぉ? 150???대궡濡??낅젰??二쇱꽭??"); return; }

    if (postType === POST_TYPE.FREE) {
      if (isTiptapEmpty(content)) { toast.error("?댁슜???낅젰??二쇱꽭??"); return; }
      if (!isLoggedIn && (guestPassword.length < 4 || guestPassword.length > 72)) {
        toast.error("鍮꾨?踰덊샇??4~72?먮줈 ?낅젰??二쇱꽭??"); return;
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
        toast.error(`?좏깮吏瑜?理쒖냼 ${VOTE_OPTION_MIN}媛??댁긽 ?낅젰??二쇱꽭??`); return;
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
        留덇컧??      </label>
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
          湲고븳 ?놁쓬
        </button>
      </div>
    </div>
  );

  return (
    <PostWriteForm
      heading="湲?곌린"
      isPending={isPending}
      onSubmit={handleSubmit}
      title={title}
      onTitleChange={setTitle}
      targetMbti={targetMbti}
      onTargetMbtiChange={setTargetMbti}
      onContentChange={setContent}
      onUpload={handleUpload}
      mbtiList={mbtiList}
      beforeTitle={typeToggle}
      contentLabel={postType !== POST_TYPE.FREE ? "?댁슜 (?좏깮)" : undefined}
      contentPlaceholder={
        postType === POST_TYPE.VOTE ? "?ы몴??????ㅻ챸???낅젰?섏꽭??"
          : postType === POST_TYPE.DEBATE ? "?좊줎 二쇱젣??????ㅻ챸???낅젰?섏꽭??"
          : "?댁슜???낅젰?섏꽭??"
      }
    >
      {/* FREE: 鍮꾪쉶??鍮꾨?踰덊샇 */}
      {postType === POST_TYPE.FREE && !isLoggedIn && (
        <div>
          <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
            鍮꾪쉶??鍮꾨?踰덊샇
          </label>
          <input
            type="password"
            value={guestPassword}
            onChange={(e) => setGuestPassword(e.target.value)}
            placeholder="鍮꾪쉶??鍮꾨?踰덊샇 (4~72??"
            minLength={4}
            maxLength={72}
            autoComplete="off"
            disabled={isPending}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
          />
        </div>
      )}

      {/* VOTE: ?좏깮吏 + 留덇컧??*/}
      {postType === POST_TYPE.VOTE && (
        <>
          <fieldset className="border border-gray-200 rounded-lg p-4 space-y-3">
            <legend className="px-1 text-sm font-medium text-gray-700">?ы몴 ?좏깮吏</legend>

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
                MBTI ?ы몴 紐⑤뱶 {isMbtiMode ? "ON" : "OFF"}
              </button>
              {isMbtiMode && (
                <span className="text-xs text-gray-400">
                  MBTI ?좏삎蹂??좏깮吏媛 ?먮룞 ?앹꽦?⑸땲??
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
                      placeholder={`?좏깮吏 ${index + 1}`}
                      disabled={isPending}
                      className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
                    />
                  )}
                  <button
                    type="button"
                    onClick={() => removeOption(index)}
                    disabled={isPending || options.length <= VOTE_OPTION_MIN}
                    className="text-gray-400 hover:text-red-500 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-lg leading-none"
                    aria-label="?좏깮吏 ??젣"
                  >
                    횞
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
                + ?좏깮吏 異붽? ({options.length}/{VOTE_OPTION_MAX})
              </button>
            )}
          </fieldset>

          {deadlineField}
        </>
      )}

      {/* DEBATE: 李щ컲 ?덉씠釉?+ 留덇컧??*/}
      {postType === POST_TYPE.DEBATE && (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                李ъ꽦 ?덉씠釉?(?좏깮)
              </label>
              <input
                type="text"
                value={proLabel}
                onChange={(e) => setProLabel(e.target.value)}
                placeholder="李ъ꽦"
                maxLength={50}
                disabled={isPending}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                諛섎? ?덉씠釉?(?좏깮)
              </label>
              <input
                type="text"
                value={conLabel}
                onChange={(e) => setConLabel(e.target.value)}
                placeholder="諛섎?"
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

