"use client";

import { useRouter } from "next/navigation";
import RichContentEditor from "@/components/post/RichContentEditor";
import { NONE_MBTI_ID, type MbtiMetadata } from "@/types/mbti";

interface Props {
  heading: string;
  isPending: boolean;
  onSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
  title: string;
  onTitleChange: (value: string) => void;
  targetMbti: string;
  onTargetMbtiChange: (value: string) => void;
  onContentChange: (value: string) => void;
  onUpload?: (url: string) => void;
  mbtiList: MbtiMetadata[];
  contentLabel?: string;
  contentPlaceholder?: string;
  beforeTitle?: React.ReactNode;
  children?: React.ReactNode;
}

/**
 * 게시글 작성 공용 폼 셸 — 제목·대상MBTI·내용 에디터·버튼 렌더링
 * 주의: 게시판별 추가 필드(투표 선택지, 비회원 비밀번호 등)는 children으로 전달
 */
export default function PostWriteForm({
  heading,
  isPending,
  onSubmit,
  title,
  onTitleChange,
  targetMbti,
  onTargetMbtiChange,
  onContentChange,
  onUpload,
  mbtiList,
  contentLabel,
  contentPlaceholder = "내용을 입력하세요.",
  beforeTitle,
  children,
}: Props) {
  const router = useRouter();

  return (
    <section className="max-w-2xl mx-auto px-4 py-6">
      <h1 className="text-xl font-bold text-gray-900 mb-6">{heading}</h1>

      <form onSubmit={onSubmit} className="space-y-5">
        {beforeTitle}
        <input
          type="text"
          value={title}
          onChange={(e) => onTitleChange(e.target.value)}
          maxLength={150}
          placeholder="제목"
          disabled={isPending}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
        />

        <div>
          <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
            대상 MBTI
          </label>
          <select
            value={targetMbti}
            onChange={(e) => onTargetMbtiChange(e.target.value)}
            disabled={isPending}
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
          >
            <option value="ALL">전체</option>
            {mbtiList
              .filter((m) => m.id !== NONE_MBTI_ID)
              .map((m) => (
                <option key={m.id} value={m.id}>{m.id}</option>
              ))}
          </select>
        </div>

        {contentLabel ? (
          <div>
            <label className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
              {contentLabel}
            </label>
            <RichContentEditor onChange={onContentChange} placeholder={contentPlaceholder} onUpload={onUpload} />
          </div>
        ) : (
          <RichContentEditor onChange={onContentChange} placeholder={contentPlaceholder} onUpload={onUpload} />
        )}

        {children}

        <div className="flex gap-2 justify-end">
          <button
            type="button"
            onClick={() => router.back()}
            disabled={isPending}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="rounded-lg bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
          >
            {isPending ? "등록 중..." : "등록"}
          </button>
        </div>
      </form>
    </section>
  );
}
