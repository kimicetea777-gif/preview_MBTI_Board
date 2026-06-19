import { Suspense } from "react";
import AdminUserStatsContent from "@/components/admin/AdminUserStatsContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminUserStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminUserStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
