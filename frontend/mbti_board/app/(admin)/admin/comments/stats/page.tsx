"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminCommentStatsContent = dynamic(() => import("@/components/admin/AdminCommentStatsContent"), { ssr: false });

export default function AdminCommentStatsPage() {
  return (
    <ApiErrorBoundary>
      <Suspense fallback={<LoadingIndicator />}>
        <AdminCommentStatsContent />
      </Suspense>
    </ApiErrorBoundary>
  );
}
