"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminPostStatsContent = dynamic(() => import("@/components/admin/AdminPostStatsContent"), { ssr: false });

export default function AdminPostStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminPostStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
