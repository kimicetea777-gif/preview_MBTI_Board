import { Suspense } from "react";
import AdminCommentStatsContent from "@/components/admin/AdminCommentStatsContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminCommentStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminCommentStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
