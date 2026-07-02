"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminCommentTable = dynamic(() => import("@/components/admin/AdminCommentTable"), { ssr: false });

export default function AdminCommentsPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">댓글 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminCommentTable />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
