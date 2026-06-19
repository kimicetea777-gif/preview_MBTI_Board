"use client";

import { notFound } from "next/navigation";
import { MbtiDetail } from "@/components/mbti/MbtiDetail";
import { existsMbti } from "@/constants/mbtiData";

interface Props {
  id: string;
}

export default function MbtiDetailContent({ id }: Props) {
  if (!existsMbti(id)) notFound();

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <MbtiDetail
        mbtiId={id}
      />
    </div>
  );
}
