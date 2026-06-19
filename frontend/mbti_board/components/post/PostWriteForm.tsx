"use client";

import { useRouter } from "next/navigation";
import RichContentEditor from "@/components/post/RichContentEditor";
import MbtiSelect from "@/components/common/MbtiSelect";
import FloatingLabelInput from "@/components/common/FloatingLabelInput";
import { MBTI_ALL, type MbtiMetadata } from "@/types/mbti";
import { MAX_TITLE_LENGTH } from "@/types/post";

interface Props {
  isPending: boolean;
  isUploading?: boolean;
  onSubmit: (e: React.SyntheticEvent<HTMLFormElement>) => void;
  title: string;
  onTitleChange: (value: string) => void;
  targetMbti: string;
  onTargetMbtiChange: (value: string) => void;
  onContentChange: (value: string) => void;
  onUpload?: (url: string) => void;
  onUploadingChange?: (count: number) => void;
  mbtiList: MbtiMetadata[];
  contentLabel?: string;
  contentPlaceholder?: string;
  children?: React.ReactNode;
}

/**
 * 게시글 작성 공용 폼 셸 — 제목·대상MBTI·내용 에디터·버튼 렌더링
 * 주의: 게시판별 추가 필드(투표 선택지, 비회원 비밀번호 등)는 children으로 전달
 */
export default function PostWriteForm({
  isPending,
  isUploading = false,
  onSubmit,
  title,
  onTitleChange,
  targetMbti,
  onTargetMbtiChange,
  onContentChange,
  onUpload,
  onUploadingChange,
  mbtiList,
  contentLabel,
  contentPlaceholder = "내용을 입력하세요.",
  children,
}: Props) {
  const router = useRouter();

  return (
    <div className="bg-card border border-border rounded-3xl shadow-[0_4px_20px_-2px_rgba(0,0,0,0.08)] p-6 md:p-10">
      <form onSubmit={onSubmit} className="space-y-6">
        {/* Title */}
        <FloatingLabelInput
          id="title"
          label="제목을 입력해요"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          maxLength={MAX_TITLE_LENGTH}
          disabled={isPending}
        />
        {/* Target MBTI */}
        <div className="flex items-center gap-3">
          <label className="text-sm font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap px-1">
            이 유형의 MBTI가 봤으면 좋겠어요  :
          </label>
          <MbtiSelect
            value={targetMbti === MBTI_ALL ? undefined : targetMbti}
            onChange={(v) => onTargetMbtiChange(v ?? MBTI_ALL)}
            mbtiList={mbtiList}
            placeholder="모든 MBTI"
            disabled={isPending}
          />
        </div>

        {contentLabel ? (
          <div className="space-y-1.5">
            <RichContentEditor onChange={onContentChange} placeholder={contentPlaceholder} onUpload={onUpload} onUploadingChange={onUploadingChange} />
          </div>
        ) : (
          <RichContentEditor onChange={onContentChange} placeholder={contentPlaceholder} onUpload={onUpload} onUploadingChange={onUploadingChange} />
        )}

        {children}

        <div className="flex flex-col-reverse sm:flex-row items-center justify-end gap-3 pt-4 border-t border-border">
          <button
            type="button"
            onClick={() => router.back()}
            disabled={isPending}
            className="w-full sm:w-auto px-8 py-2.5 rounded-full border border-primary text-primary text-sm font-semibold hover:bg-surface-container transition-all disabled:opacity-50"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={isPending}
            className={`w-full sm:w-auto px-10 py-2.5 rounded-full bg-primary text-primary-foreground text-sm font-semibold shadow-md shadow-primary/20 hover:opacity-90 active:scale-95 transition-all disabled:opacity-50 ${isUploading ? "opacity-50 cursor-not-allowed" : ""}`}
          >
            {isPending ? "등록 중..." : "등록하기"}
          </button>
        </div>
      </form>
    </div>
  );
}
