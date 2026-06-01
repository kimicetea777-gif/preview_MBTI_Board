"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchMbtiList } from "@/lib/api/mbti";
import { POST_SORT } from "@/types/post";
import MbtiSelect from "@/components/common/MbtiSelect";
import LabeledFilterBox from "@/components/common/LabeledFilterBox";
import BoardTypeNav from "@/components/post/BoardTypeNav";
import type { PostSortType } from "@/types/post";

const SORT_OPTIONS: { label: string; value: PostSortType }[] = [
  { label: "최신순", value: POST_SORT.LATEST },
  { label: "댓글많은순", value: POST_SORT.COMMENTS },
  { label: "조회많은순", value: POST_SORT.VIEWS },
];

interface Props {
  sort: PostSortType;
  authorMbti: string | undefined;
  targetMbti: string | undefined;
  onSortChange: (sort: PostSortType) => void;
  onAuthorMbtiChange: (mbti: string | undefined) => void;
  onTargetMbtiChange: (mbti: string | undefined) => void;
}

export default function PostListFilter({
  sort,
  authorMbti,
  targetMbti,
  onSortChange,
  onAuthorMbtiChange,
  onTargetMbtiChange,
}: Props) {
  const { data: mbtiList = [] } = useQuery({
    queryKey: ["mbtiList"],
    queryFn: fetchMbtiList,
    staleTime: Infinity,
  });

  return (
    <nav className="flex flex-col gap-2 py-3">
      <BoardTypeNav />

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex rounded-lg border border-gray-200 overflow-hidden text-xs">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => onSortChange(opt.value)}
              className={`px-3 py-1.5 transition-colors ${
                sort === opt.value
                  ? "bg-gray-900 text-gray-50 "
                  : "bg-white text-gray-600 hover:bg-gray-50"
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3">
          <LabeledFilterBox label="작성자 MBTI">
            <MbtiSelect
              value={authorMbti ?? ""}
              onChange={(v) => onAuthorMbtiChange(v || undefined)}
              mbtiList={mbtiList}
              placeholder="전체"
              className="border-0 rounded-none flex-1 py-1.5"
            />
          </LabeledFilterBox>

          <LabeledFilterBox label="대상 MBTI">
            <MbtiSelect
              value={targetMbti ?? ""}
              onChange={(v) => onTargetMbtiChange(v || undefined)}
              mbtiList={mbtiList}
              placeholder="전체"
              className="border-0 rounded-none flex-1 py-1.5"
            />
          </LabeledFilterBox>
        </div>
      </div>
    </nav>
  );
}
