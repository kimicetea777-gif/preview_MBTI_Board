import { Suspense } from "react";
import AdminMbtiContent from "@/components/admin/AdminMbtiContent";
import ApiErrorBoundary from "@/components/common/ApiErrorBoundary";

export default function AdminMbtiPage() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold text-foreground mb-6">MBTI 문제 관리</h1>
      <ApiErrorBoundary>
        <Suspense fallback={<p className="py-8 text-center text-gray-500">불러오는 중...</p>}>
          <AdminMbtiContent />
        </Suspense>
      </ApiErrorBoundary>
    </div>
  );
}
