import { useState } from "react";
import { useRouter } from "next/navigation";
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
  const queryClient = useQueryClient();

  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [modalAction, setModalAction] = useState<ModalAction>(MODAL_ACTION.UPDATE);
  const [modalError, setModalError] = useState<string | undefined>();

  const updateMutation = useMutation({
    mutationFn: (req: PostUpdateRequest) => updatePost(id, req),
    onSuccess: () => {
      setIsEditing(false);
      setModalOpen(false);
      queryClient.invalidateQueries({ queryKey: postKeys.detail(id) });
      toast.success("게시글이 수정되었습니다.");
    },
    onError: (err) => {
      if (post?.isGuest) {
        setModalError(getApiErrorMessage(err));
      } else {
        toast.error(getApiErrorMessage(err));
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (guestPassword: string | undefined) => deletePost(id, guestPassword),
    onSuccess: () => router.push(redirectPath),
    onError: (err) => setModalError(getApiErrorMessage(err)),
  });

  const handleEditStart = () => {
    if (!post) return;
    setEditTitle(post.title);
    setEditContent(post.content ?? "");
    setIsEditing(true);
  };

  const handleEditCancel = () => setIsEditing(false);

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
