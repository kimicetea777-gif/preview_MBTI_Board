import { Suspense } from "react";
import AdminDashboardContent from "@/components/admin/AdminDashboardContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminPage() {
  return (
    <ApiErrorBoundary>
      <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
        <AdminDashboardContent />
      </Suspense>
    </ApiErrorBoundary>
  );
}
