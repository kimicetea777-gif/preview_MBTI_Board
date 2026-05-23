"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchMbtiList } from "@/lib/api/mbti";
import { POST_SORT } from "@/types/post";
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
    <nav className="flex flex-wrap items-center gap-3 py-3">
      <div className="flex rounded-lg border border-gray-200 overflow-hidden text-xs">
        {SORT_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => onSortChange(opt.value)}
            className={`px-3 py-1.5 transition-colors ${
              sort === opt.value
                ? "bg-gray-900 text-white"
                : "bg-white text-gray-600 hover:bg-gray-50"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <select
        value={authorMbti ?? ""}
        onChange={(e) => onAuthorMbtiChange(e.target.value || undefined)}
        className="rounded-lg border border-gray-200 px-2 py-1.5 text-xs text-gray-600 focus:outline-none focus:border-gray-400"
      >
        <option value="">작성자 MBTI 전체</option>
        {mbtiList.map((m) => (
          <option key={m.id} value={m.id}>{m.id}</option>
        ))}
      </select>

      <select
        value={targetMbti ?? ""}
        onChange={(e) => onTargetMbtiChange(e.target.value || undefined)}
        className="rounded-lg border border-gray-200 px-2 py-1.5 text-xs text-gray-600 focus:outline-none focus:border-gray-400"
      >
        <option value="">대상 MBTI 전체</option>
        {mbtiList.map((m) => (
          <option key={m.id} value={m.id}>{m.id}</option>
        ))}
      </select>
    </nav>
  );
}
