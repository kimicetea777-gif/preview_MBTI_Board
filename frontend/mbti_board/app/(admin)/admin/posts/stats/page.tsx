import { Suspense } from "react";
import AdminPostStatsContent from "@/components/admin/AdminPostStatsContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminPostStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminPostStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
