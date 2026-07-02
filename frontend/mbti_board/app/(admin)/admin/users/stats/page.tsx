"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminUserStatsContent = dynamic(() => import("@/components/admin/AdminUserStatsContent"), { ssr: false });

export default function AdminUserStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminUserStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
