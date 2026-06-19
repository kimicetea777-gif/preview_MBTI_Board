import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { deletePost } from "@/lib/api/post";
import type { PostMenuItem } from "@/components/post/PostMenuButton";
import { toast } from "@/components/common/Toast";
import { MdEdit, MdDelete } from "react-icons/md";

interface PostForDelete {
  isOwner: boolean;
}

interface Options {
  id: number;
  post: PostForDelete | null | undefined;
  redirectPath: string;
  editUnavailableMessage: string;
}

export function usePostDelete({ id, post, redirectPath, editUnavailableMessage }: Options) {
  const router = useRouter();

  const deleteMutation = useMutation({
    mutationFn: () => deletePost(id, undefined),
    onSuccess: () => router.push(redirectPath),
    onError: () => toast.error("삭제 중 오류가 발생했습니다."),
  });

  const menuItems: PostMenuItem[] | undefined = post?.isOwner
    ? [
        {
          label: "수정하기",
          icon: <MdEdit className="text-base" />,
          onClick: () => toast.info(editUnavailableMessage),
        },
        {
          label: deleteMutation.isPending ? "삭제 중..." : "삭제하기",
          icon: <MdDelete className="text-base" />,
          onClick: () => {
            if (!window.confirm("게시글을 삭제하시겠습니까?")) return;
            deleteMutation.mutate();
          },
          disabled: deleteMutation.isPending,
          destructive: true,
        },
      ]
    : undefined;

  return { menuItems };
}
