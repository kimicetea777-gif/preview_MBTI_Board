"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminMbtiContent = dynamic(() => import("@/components/admin/AdminMbtiContent"), { ssr: false });

export default function AdminMbtiPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">MBTI 문제 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminMbtiContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
