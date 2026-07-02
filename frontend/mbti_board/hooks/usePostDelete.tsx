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
  /** "수정하기" 클릭 시 보여줄 안내 메시지 — 투표/토론은 선택지·찬반 구조 때문에 사후 수정 미지원 */
  editUnavailableMessage: string;
}

/**
 * 투표·토론 게시글 전용 삭제 훅
 * 주의: 자유글(usePostEditDelete)과 달리 인라인 수정을 지원하지 않음 —
 *       "수정하기" 메뉴는 UI 일관성을 위해 노출만 하고 클릭 시 안내 토스트만 띄움
 */
export function usePostDelete({ id, post, redirectPath, editUnavailableMessage }: Options) {
  const router = useRouter();

  // 삭제 성공 시 redirectPath(목록 등)로 이동
  const deleteMutation = useMutation({
    mutationFn: () => deletePost(id, undefined),
    onSuccess: () => router.push(redirectPath),
    onError: () => toast.error("삭제 중 오류가 발생했습니다."),
  });

  // 작성자 본인일 때만 케밥 메뉴(수정/삭제) 노출, 비회원·타인은 undefined로 메뉴 자체를 숨김
  const menuItems: PostMenuItem[] | undefined = post?.isOwner
    ? [
        {
          label: "수정하기",
          icon: <MdEdit className="text-base" />,
          // 실제 수정 페이지로 이동하지 않고 수정 불가 안내만 표시
          onClick: () => toast.info(editUnavailableMessage),
        },
        {
          label: deleteMutation.isPending ? "삭제 중..." : "삭제하기",
          icon: <MdDelete className="text-base" />,
          onClick: () => {
            // 삭제는 되돌릴 수 없으므로 confirm으로 1차 확인
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
