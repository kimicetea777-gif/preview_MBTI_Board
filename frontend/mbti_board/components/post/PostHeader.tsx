"use client";

import PostAuthorMeta from "@/components/post/PostAuthorMeta";
import PostMenuButton, { type PostMenuItem } from "@/components/post/PostMenuButton";
import type { MbtiVerified } from "@/types/user";

interface Props {
  title: string;
  isEditing?: boolean;
  editTitle?: string;
  onEditTitleChange?: (value: string) => void;
  authorMbtiId: string;
  mbtiVerified: MbtiVerified;
  authorNickname: string;
  createdAt: string;
  viewCount: number;
  menuItems?: PostMenuItem[];
  targetMbtiId?: string | null;
}

export default function PostHeader({
  title,
  isEditing = false,
  editTitle = "",
  onEditTitleChange,
  authorMbtiId,
  mbtiVerified,
  authorNickname,
  createdAt,
  viewCount,
  menuItems,
  targetMbtiId,
}: Props) {
  return (
    <header className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 flex items-start gap-4 min-w-0">
          {isEditing ? (
            <input
              type="text"
              value={editTitle}
              onChange={(e) => onEditTitleChange?.(e.target.value)}
              maxLength={150}
              className="flex-1 rounded-lg border border-outline-variant bg-surface-container-lowest px-4 py-3 text-2xl md:text-3xl font-extrabold text-primary focus:border-secondary focus:outline-none focus:ring-2 focus:ring-secondary/30 transition-all"
            />
          ) : (
            <h1 className="flex-1 text-3xl md:text-4xl font-extrabold text-primary leading-tight break-words">
              {title}
            </h1>
          )}
          {!isEditing && targetMbtiId && (
            <div className="flex flex-col items-center border border-secondary rounded px-3 py-1 bg-secondary-container/20 min-w-14 shrink-0 mt-1">
              <span className="text-[13px] font-bold tracking-widest text-on-surface-variant uppercase">wanted</span>
              <span className="text-lg font-bold text-on-secondary-container leading-tight">{targetMbtiId}</span>
            </div>
          )}
        </div>
        {!isEditing && menuItems && menuItems.length > 0 && (
          <div className="shrink-0 pr-8">
            <PostMenuButton items={menuItems} />
          </div>
        )}
      </div>
      <PostAuthorMeta
        authorMbtiId={authorMbtiId}
        mbtiVerified={mbtiVerified}
        authorNickname={authorNickname}
        createdAt={createdAt}
        viewCount={viewCount}
      />
    </header>
  );
}
