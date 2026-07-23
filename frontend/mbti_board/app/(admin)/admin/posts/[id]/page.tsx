"use client";

import { useParams } from "next/navigation";
import { Suspense } from "react";
import dynamic from "next/dynamic";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

const AdminPostDetailContent = dynamic(() => import("@/components/admin/AdminPostDetailContent"), { ssr: false });

export default function AdminPostDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  if (!Number.isInteger(id) || id <= 0) {
    return <div className="p-6 text-sm text-destructive">잘못된 게시글 ID입니다.</div>;
  }

  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<LoadingIndicator />}>
          <AdminPostDetailContent id={id} />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
