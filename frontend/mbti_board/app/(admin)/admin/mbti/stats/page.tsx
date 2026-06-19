import { Suspense } from "react";
import AdminMbtiTestStatsContent from "@/components/admin/AdminMbtiTestStatsContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminMbtiStatsPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminMbtiTestStatsContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
