"use client";

import nextDynamic from "next/dynamic";

const MyAccountContent = nextDynamic(() => import("@/components/my/account/MyAccountContent"), { ssr: false });

export const dynamic = "force-dynamic";

export default function MyAccountPage() {
  return (
    <section className="p-6 pb-24">
      <MyAccountContent />
    </section>
  );
}
