import { POST_TYPE, POST_TYPE_LABELS } from "@/types/post";
import type { PostType } from "@/types/post";

const TABS: { label: string; value: PostType | undefined }[] = [
  { label: "전체",                             value: undefined },
  { label: POST_TYPE_LABELS[POST_TYPE.FREE],   value: POST_TYPE.FREE },
  { label: POST_TYPE_LABELS[POST_TYPE.VOTE],   value: POST_TYPE.VOTE },
  { label: POST_TYPE_LABELS[POST_TYPE.DEBATE], value: POST_TYPE.DEBATE },
];

interface Props {
  totalElements: number;
  selectedType: PostType | undefined;
  onTabChange: (type: PostType | undefined) => void;
}

export default function MyPostsHeader({ totalElements, selectedType, onTabChange }: Props) {
  return (
    <div className="flex flex-col gap-3 mb-6 sm:flex-row sm:items-center sm:justify-between">
      <h1 className="text-2xl font-bold text-primary flex items-baseline gap-2">
        내 게시글
        <span className="text-sm font-normal text-on-surface-variant">{totalElements}개</span>
      </h1>

      <div className="flex items-center gap-1 p-1 bg-surface-container rounded-xl self-start">
        {TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            onClick={() => onTabChange(tab.value)}
            className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
              selectedType === tab.value
                ? "bg-surface-container-lowest text-primary shadow-sm"
                : "text-on-surface-variant hover:text-on-surface"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}
