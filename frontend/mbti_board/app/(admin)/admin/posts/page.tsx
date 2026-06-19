import { Suspense } from "react";
import AdminPostTable from "@/components/admin/AdminPostTable";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminPostsPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">게시글 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminPostTable />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
