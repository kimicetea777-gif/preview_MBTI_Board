"use client";

import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminPostTable = dynamic(() => import("@/components/admin/AdminPostTable"), { ssr: false });

export default function AdminPostsPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">게시글 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminPostTable />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
