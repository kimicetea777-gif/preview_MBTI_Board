import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { formatRelativeTime } from "@/lib/dateUtils";
import { postHref } from "@/lib/boardUtils";
import { toast } from "@/components/common/Toast";
import PostTypeBadge from "@/components/common/PostTypeBadge";
import PostMenuButton, { type PostMenuItem } from "@/components/post/PostMenuButton";
import { usePagination, PAGE_ELLIPSIS } from "@/hooks/usePagination";
import { POST_TYPE } from "@/types/post";
import type { PageResponse } from "@/types/api";
import type { PostSummaryResponse, PostType } from "@/types/post";
import {
  MdChatBubble, MdBarChart,
  MdEdit, MdDelete, MdChevronLeft, MdChevronRight,
} from "react-icons/md";

// 투표·토론 게시글은 사후 수정 미지원 — VoteDetailContent·DebateDetailContent의 editUnavailableMessage와 동일 문구
const EDIT_UNAVAILABLE_MESSAGE: Partial<Record<PostType, string>> = {
  [POST_TYPE.VOTE]: "투표 게시글은 수정할 수 없습니다.",
  [POST_TYPE.DEBATE]: "토론 게시글은 수정할 수 없습니다.",
};

interface PostActionsOptions {
  router: ReturnType<typeof useRouter>;
  onDelete: (id: number, title: string) => void;
  isDeleting: boolean;
}

// 게시글 하나의 수정·삭제 메뉴 항목 생성 — .map() 루프 안에서 게시글별로 호출되므로 훅이 아닌 순수 함수로 분리
function buildPostActionItems(
  post: PostSummaryResponse,
  { router, onDelete, isDeleting }: PostActionsOptions
): { editItem: PostMenuItem; deleteItem: PostMenuItem } {
  const handleEditClick = () => {
    const unavailableMessage = EDIT_UNAVAILABLE_MESSAGE[post.typeCode];
    if (unavailableMessage) {
      toast.info(unavailableMessage);
      return;
    }
    router.push(`${postHref(post.typeCode, post.id)}?edit=1`);
  };

  return {
    editItem: { label: "수정", icon: <MdEdit />, onClick: handleEditClick },
    deleteItem: {
      label: "삭제",
      icon: <MdDelete />,
      onClick: () => onDelete(post.id, post.title),
      disabled: isDeleting,
      destructive: true,
    },
  };
}

interface Props {
  data: PageResponse<PostSummaryResponse>;
  page: number;
  onPageChange: (page: number) => void;
  onDelete: (id: number, title: string) => void;
  isDeleting: boolean;
}

export default function MyPostsList({ data, page, onPageChange, onDelete, isDeleting }: Props) {
  const router = useRouter();
  const { pageNumbers, goToPrev, goToNext } = usePagination({
    page,
    totalPages: data.totalPages,
    onPageChange,
  });

  if (data.content.length === 0) {
    return <p className="py-16 text-center text-sm text-on-surface-variant">아직 작성한 게시글이 없습니다.</p>;
  }

  return (
    <>
      <div className="flex flex-col border-t border-outline-variant/20">
        {data.content.map((post) => {
          const detailHref = postHref(post.typeCode, post.id);
          const thumbnail = post.imageUrls?.[0];
          const { editItem, deleteItem } = buildPostActionItems(post, { router, onDelete, isDeleting });
          const menuItems: PostMenuItem[] = [editItem, deleteItem];

          return (
            <article
              key={post.id}
              className="group flex items-center gap-4 p-1 border-b border-outline-variant/20 hover:bg-surface-container-low transition-colors"
            >
              {/* 썸네일 */}
              <div className="relative w-16 h-16 shrink-0 rounded-xl overflow-hidden">
                {thumbnail ? (
                  <Image
                    src={thumbnail}
                    alt=""
                    fill
                    sizes="64px"
                    className="object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <div className="w-full h-full border-2 border-dashed border-outline-variant rounded-xl" />
                )}
              </div>

              {/* 본문 */}
              <div className="grow min-w-0 flex flex-col justify-between gap-2">
                <div className="flex flex-col gap-1">
                  <div className="flex items-center justify-between gap-2">
                    <PostTypeBadge type={post.typeCode} />
                    <div className="sm:hidden">
                      <PostMenuButton items={menuItems} />
                    </div>
                  </div>
                  <Link href={detailHref} className="block hover:underline min-w-0">
                    <p className="text-sm font-semibold text-primary truncate">{post.title}</p>
                  </Link>
                </div>

                <div className="flex items-center gap-4 text-xs text-on-surface-variant">
                  <span className="w-12 shrink-0">{formatRelativeTime(post.createdAt)}</span>
                  <span className="flex items-center gap-1">
                    <MdBarChart className="text-base" />
                    {post.viewCount}
                  </span>
                  <span className="flex items-center gap-1">
                    <MdChatBubble className="text-base" />
                    {post.commentCount}
                  </span>
                </div>
              </div>

              {/* 수정·삭제 (작은 화면에서는 배지 옆 드롭다운 메뉴로 대체) */}
              <div className="hidden sm:flex shrink-0 items-center gap-1">
                <button
                  type="button"
                  onClick={editItem.onClick}
                  title="수정"
                  className="p-2 rounded-full text-on-surface-variant hover:bg-surface-container-highest transition-colors"
                >
                  <MdEdit className="text-lg" />
                </button>
                <button
                  type="button"
                  onClick={deleteItem.onClick}
                  disabled={deleteItem.disabled}
                  title="삭제"
                  className="p-2 rounded-full text-error hover:bg-error-container/30 transition-colors disabled:opacity-40"
                >
                  <MdDelete className="text-lg" />
                </button>
              </div>
            </article>
          );
        })}
      </div>

      {/* 페이지네이션 */}
      {data.totalPages > 1 && (
        <nav className="mt-12 flex justify-center items-center gap-2">
          <button
            type="button"
            onClick={goToPrev}
            disabled={page === 0}
            className="w-10 h-10 flex items-center justify-center rounded-full border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-30"
          >
            <MdChevronLeft className="text-xl" />
          </button>

          {pageNumbers.map((num, i) =>
            num === PAGE_ELLIPSIS ? (
              <span key={`ellipsis-${i}`} className="w-10 h-10 flex items-center justify-center text-on-surface-variant">
                ...
              </span>
            ) : (
              <button
                key={num}
                type="button"
                onClick={() => onPageChange(num)}
                className={`w-10 h-10 flex items-center justify-center rounded-full text-sm font-semibold transition-colors ${
                  page === num
                    ? "bg-primary text-on-primary"
                    : "text-on-surface-variant hover:bg-surface-container"
                }`}
              >
                {num + 1}
              </button>
            )
          )}

          <button
            type="button"
            onClick={goToNext}
            disabled={data.last}
            className="w-10 h-10 flex items-center justify-center rounded-full border border-outline-variant text-on-surface-variant hover:bg-surface-container transition-colors disabled:opacity-30"
          >
            <MdChevronRight className="text-xl" />
          </button>
        </nav>
      )}
    </>
  );
}
