"use client";

import type { MbtiMetadata } from "@/types/mbti";
import MbtiAvatar from "@/components/common/MbtiAvatar";

interface Props {
  mbti: MbtiMetadata;
}

export default function MbtiTypePreviewCard({ mbti }: Props) {
  return (
    <article className="flex flex-col items-center rounded-xl bg-surface-container-low p-4 text-center shadow-lg">
      <MbtiAvatar mbti={mbti} size="lg" className="-mb-4" />
      <h3 className="mb-3 text-base font-bold" style={{ color: mbti.colorCode }}>
        {mbti.id}
      </h3>
      <ul className="w-fit space-y-1.5 text-left">
        {mbti.personalities.map(({ label, icon: Icon }) => (
          <li key={label} className="flex items-center gap-2 text-xs text-muted-foreground">
            <Icon size={13} className="shrink-0" style={{ color: mbti.colorCode }} />
            <span>{label}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}
