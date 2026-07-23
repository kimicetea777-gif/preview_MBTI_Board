import { useEffect, useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { getApiErrorMessage } from "@/lib/apiError";
import { updatePost, deletePost } from "@/lib/api/post";
import { postKeys } from "@/hooks/usePosts";
import { isTiptapEmpty } from "@/lib/editor";
import { MODAL_ACTION, type ModalAction } from "@/components/post/GuestPasswordModal";
import type { PostMenuItem } from "@/components/post/PostMenuButton";
import type { PostUpdateRequest } from "@/types/post";
import { MdEdit, MdDelete } from "react-icons/md";
import { toast } from "@/components/common/Toast";

interface PostForActions {
  title: string;
  content?: string | null;
  isOwner: boolean;
  isGuest: boolean;
}

interface Options {
  id: number;
  post: PostForActions | null | undefined;
  redirectPath: string;
}

export function usePostEditDelete({ id, post, redirectPath }: Options) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  // 목록에서 "?edit=1"로 진입 시 편집 폼을 바로 연 상태로 초기화 — effect 내 동기 setState 방지
  const [isEditing, setIsEditing] = useState(
    () => searchParams.get("edit") === "1" && !!post && (post.isOwner || post.isGuest)
  );
  const [editTitle, setEditTitle] = useState(() => (isEditing && post ? post.title : ""));
  const [editContent, setEditContent] = useState(() => (isEditing && post ? post.content ?? "" : ""));
  const [modalOpen, setModalOpen] = useState(false);
  const [modalAction, setModalAction] = useState<ModalAction>(MODAL_ACTION.UPDATE);
  const [modalError, setModalError] = useState<string | undefined>();

  // 게스트는 모달이 열려있는 채로 에러를 받으므로 모달 안에 표시, 회원은 모달 없이 바로 실행되므로 토스트로 표시
  const handleMutationError = (err: unknown) => {
    if (post?.isGuest) {
      setModalError(getApiErrorMessage(err));
    } else {
      toast.error(getApiErrorMessage(err));
    }
  };

  const updateMutation = useMutation({
    mutationFn: (req: PostUpdateRequest) => updatePost(id, req),
    onSuccess: () => {
      setIsEditing(false);
      setModalOpen(false);
      queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
      toast.success("게시글이 수정되었습니다.");
    },
    onError: handleMutationError,
  });

  const deleteMutation = useMutation({
    mutationFn: (guestPassword: string | undefined) => deletePost(id, guestPassword),
    onSuccess: () => router.push(redirectPath),
    onError: handleMutationError,
  });

  const handleEditStart = () => {
    if (!post) return;
    setEditTitle(post.title);
    setEditContent(post.content ?? "");
    setIsEditing(true);
  };

  const handleEditCancel = () => setIsEditing(false);

  // 편집 폼을 연 초기 상태로 만든 뒤에는 URL에 남은 "?edit=1"만 정리
  useEffect(() => {
    if (searchParams.get("edit") !== "1") return;
    const params = new URLSearchParams(searchParams.toString());
    params.delete("edit");
    router.replace(params.toString() ? `${pathname}?${params}` : pathname, { scroll: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSaveClick = () => {
    if (!post || !editTitle.trim() || isTiptapEmpty(editContent)) {
      toast.warning("제목과 내용을 입력해주세요.");
      return;
    }
    if (post.isGuest) {
      setModalAction(MODAL_ACTION.UPDATE);
      setModalError(undefined);
      setModalOpen(true);
    } else {
      updateMutation.mutate({ title: editTitle.trim(), content: editContent });
    }
  };

  const handleDeleteClick = () => {
    if (!post) return;
    if (post.isGuest) {
      setModalAction(MODAL_ACTION.DELETE);
      setModalError(undefined);
      setModalOpen(true);
    } else {
      if (!window.confirm("게시글을 삭제하시겠습니까?")) return;
      deleteMutation.mutate(undefined);
    }
  };

  const handleModalConfirm = (password: string) => {
    setModalError(undefined);
    if (modalAction === MODAL_ACTION.UPDATE) {
      updateMutation.mutate({ title: editTitle.trim(), content: editContent, guestPassword: password });
    } else {
      deleteMutation.mutate(password);
    }
  };

  const menuItems: PostMenuItem[] | undefined = post && (post.isOwner || post.isGuest)
    ? [
        {
          label: "수정하기",
          icon: <MdEdit className="text-base" />,
          onClick: handleEditStart,
        },
        {
          label: deleteMutation.isPending ? "삭제 중..." : "삭제하기",
          icon: <MdDelete className="text-base" />,
          onClick: handleDeleteClick,
          disabled: deleteMutation.isPending,
          destructive: true,
        },
      ]
    : undefined;

  const guestModalProps = {
    open: modalOpen,
    action: modalAction,
    onConfirm: handleModalConfirm,
    onClose: () => setModalOpen(false),
    isPending: updateMutation.isPending || deleteMutation.isPending,
    error: modalError,
  };

  return {
    isEditing,
    editTitle,
    setEditTitle,
    editContent,
    setEditContent,
    handleEditCancel,
    handleSaveClick,
    updateMutation,
    menuItems,
    guestModalProps,
  };
}
