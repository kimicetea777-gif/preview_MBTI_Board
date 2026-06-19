import { Suspense } from "react";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import FunStatsContent from "./FunStatsContent";

export default function FunStatsPage() {
  return (
    <article className="mt-5 mb-10">
      <QueryResetErrorBoundary>
        <Suspense fallback={<FunStatsLoading />}>
          <FunStatsContent />
        </Suspense>
      </QueryResetErrorBoundary>
    </article>
  );
}

function FunStatsLoading() {
  return (
    <div className="flex flex-col gap-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="h-14 rounded-2xl bg-muted animate-pulse" />
      ))}
    </div>
  );
}
