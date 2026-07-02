"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminNoticeContent = dynamic(() => import("@/components/admin/AdminNoticeContent"), { ssr: false });

export default function AdminNoticesPage() {
  return (
    <ApiErrorBoundary>
      <Suspense fallback={<LoadingIndicator />}>
        <AdminNoticeContent />
      </Suspense>
    </ApiErrorBoundary>
  );
}
