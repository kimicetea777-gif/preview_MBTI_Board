import { Suspense } from "react";
import AdminUserTable from "@/components/admin/AdminUserTable";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminUsersPage() {
  return (
    <div className="p-6">
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminUserTable />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
