"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminDashboardContent = dynamic(() => import("@/components/admin/AdminDashboardContent"), { ssr: false });

export default function AdminPage() {
  return (
    <ApiErrorBoundary>
      <Suspense fallback={<LoadingIndicator />}>
        <AdminDashboardContent />
      </Suspense>
    </ApiErrorBoundary>
  );
}
