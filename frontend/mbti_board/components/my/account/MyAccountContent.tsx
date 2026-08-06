"use client";

import { Suspense } from "react";
import MyPasswordSection from "./MyPasswordSection";
import MyFindEmailSection from "./MyFindEmailSection";
import MyLogoutAllSection from "./MyLogoutAllSection";
import MyWithdrawalSection from "./MyWithdrawalSection";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";

function MyAccountContentInner() {
  return (
    <div className="flex flex-col gap-5 max-w-2xl">
      <MyPasswordSection />
      <MyFindEmailSection />
      <MyLogoutAllSection />
      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-sm">
        <h2 className="font-heading text-base font-bold text-foreground mb-4">위험 구역</h2>
        <MyWithdrawalSection />
      </div>
    </div>
  );
}

export default function MyAccountContent() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={
        <div className="flex flex-col gap-5 max-w-2xl animate-pulse">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-40 rounded-2xl bg-surface-container-low" />
          ))}
        </div>
      }>
        <MyAccountContentInner />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
