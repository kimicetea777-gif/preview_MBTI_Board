"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminAlertContent = dynamic(() => import("@/components/admin/AdminAlertContent"), { ssr: false });

export default function AdminAlertsPage() {
  return (
    <ApiErrorBoundary>
      <Suspense fallback={<LoadingIndicator />}>
        <AdminAlertContent />
      </Suspense>
    </ApiErrorBoundary>
  );
}
