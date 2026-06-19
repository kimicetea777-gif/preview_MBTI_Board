import { Suspense } from "react";
import AdminCommentTable from "@/components/admin/AdminCommentTable";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminCommentsPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">댓글 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminCommentTable />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
