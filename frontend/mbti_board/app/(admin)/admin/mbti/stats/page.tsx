"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminMbtiTestStatsContent = dynamic(() => import("@/components/admin/AdminMbtiTestStatsContent"), { ssr: false });

export default function AdminMbtiStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminMbtiTestStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
