"use client";

import nextDynamic from "next/dynamic";

const MyPageContent = nextDynamic(() => import("@/components/my/MyPageContent"), { ssr: false });

export const dynamic = "force-dynamic";

export default function MyPage() {
  return (
    <section className="p-8 pb-24">
      <MyPageContent />
    </section>
  );
}
