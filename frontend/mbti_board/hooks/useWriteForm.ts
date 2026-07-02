import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/store/authStore";
import { createPost, createVotePost, createDebatePost } from "@/lib/api/post";
import { postKeys } from "@/hooks/usePosts";
import { getAllMbti } from "@/constants/mbtiData";
import { ROUTES } from "@/constants/routes";
import { toast } from "@/components/common/Toast";
import { isTiptapEmpty, EMPTY_TIPTAP } from "@/lib/editor";
import { MBTI_ALL } from "@/types/mbti";
import {
  VOTE_OPTION_MIN,
  POST_TYPE,
  GUEST_PASSWORD_MIN_LENGTH,
  MAX_TITLE_LENGTH,
} from "@/types/post";
import type { PostType, VoteOptionRequest } from "@/types/post";
import { useImageUpload } from "@/hooks/useImageUpload";
import { getApiErrorMessage } from "@/lib/apiError";
import { useVoteOptionsForm } from "@/hooks/useVoteOptionsForm";
import { useDeadlineForm } from "@/hooks/useDeadlineForm";

export function useWriteForm(initialType: PostType) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());

  const { hasImage, handleUpload, markSaved } = useImageUpload();
  const [uploadingCount, setUploadingCount] = useState(0);

  const [postType, setPostType] = useState<PostType>(initialType);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [targetMbti, setTargetMbti] = useState(MBTI_ALL);

  const mbtiList = getAllMbti();

  // FREE
  const [guestPassword, setGuestPassword] = useState("");

  // VOTE
  const { isMbtiMode, options, toggleMbtiMode, addOption, removeOption, updateOptionText } =
    useVoteOptionsForm(mbtiList);

  // VOTE + DEBATE
  const { endsAt, isNoDeadline, setEndsAt, handleNoDeadlineToggle, validateDeadline } =
    useDeadlineForm();

  // DEBATE
  const [proLabel, setProLabel] = useState("");
  const [conLabel, setConLabel] = useState("");

  const onError = (err: unknown) => {
    toast.error(getApiErrorMessage(err) ?? "오류가 발생했습니다.");
  };

  const freeMutation = useMutation({
    mutationFn: createPost,
    onSuccess: (data) => {
      markSaved();
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.freePost(data.id));
    },
    onError,
  });

  const voteMutation = useMutation({
    mutationFn: createVotePost,
    onSuccess: (data) => {
      markSaved();
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
      router.push(ROUTES.board.votePost(data.id));
    },
    onError,
  });

  const debateMutation = useMutation({
    mutationFn: createDebatePost,
    onSuccess: (data) => {
      markSaved();
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

  const handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (uploadingCount > 0) {
      toast.info("이미지를 업로드 중입니다. 잠시만 기다려주세요.");
      return;
    }
    if (!title.trim()) { toast.error("제목을 입력해 주세요."); return; }
    if (title.trim().length > MAX_TITLE_LENGTH) {
      toast.error(`제목은 ${MAX_TITLE_LENGTH}자 이내로 입력해 주세요.`);
      return;
    }

    const resolvedTargetMbti = targetMbti === MBTI_ALL ? undefined : targetMbti;
    const resolvedEndsAt = !isNoDeadline && endsAt ? new Date(endsAt).toISOString() : undefined;

    if (postType === POST_TYPE.FREE) {
      if (isTiptapEmpty(content)) { toast.error("내용을 입력해 주세요."); return; }
      if (!isLoggedIn && guestPassword.length < GUEST_PASSWORD_MIN_LENGTH) {
        toast.error(`비밀번호는 ${GUEST_PASSWORD_MIN_LENGTH}자 이상 입력해 주세요.`);
        return;
      }
      freeMutation.mutate({
        type: POST_TYPE.FREE,
        title: title.trim(),
        content,
        targetMbti: resolvedTargetMbti,
        guestPassword: !isLoggedIn ? guestPassword : undefined,
      });
      return;
    }

    if (postType === POST_TYPE.VOTE) {
      const filled = isMbtiMode
        ? options
        : options.filter((o) => o.optionText.trim());
      if (filled.length < VOTE_OPTION_MIN) {
        toast.error(`선택지를 최소 ${VOTE_OPTION_MIN}개 이상 입력해 주세요.`);
        return;
      }
      if (!validateDeadline()) return;
      const voteOptions: VoteOptionRequest[] = filled.map((o) => ({
        optionText: isMbtiMode ? o.optionText : o.optionText.trim(),
        mbtiTarget: isMbtiMode ? o.mbtiTarget : undefined,
      }));
      voteMutation.mutate({
        title: title.trim(),
        content: content || EMPTY_TIPTAP,
        targetMbti: resolvedTargetMbti,
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
      targetMbti: resolvedTargetMbti,
      proLabel: proLabel.trim() || undefined,
      conLabel: conLabel.trim() || undefined,
      endsAt: resolvedEndsAt,
    });
  };

  return {
    postType,
    title,
    content,
    targetMbti,
    guestPassword,
    isMbtiMode,
    options,
    endsAt,
    isNoDeadline,
    proLabel,
    conLabel,
    mbtiList,
    isLoggedIn,
    hasImage,
    uploadingCount,
    isPending,
    setTitle,
    setTargetMbti,
    setContent,
    setGuestPassword,
    setEndsAt,
    setProLabel,
    setConLabel,
    handleTypeChange,
    handleNoDeadlineToggle,
    toggleMbtiMode,
    addOption,
    removeOption,
    updateOptionText,
    handleSubmit,
    handleUpload,
    setUploadingCount,
  };
}
