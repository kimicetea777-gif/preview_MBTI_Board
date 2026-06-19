import { Suspense } from "react";
import MyPostsSection from "@/components/my/MyPostsSection";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";

function MyPostsFallback() {
  return (
    <div className="p-12 pb-24 space-y-3">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-14 rounded-lg bg-surface-container animate-pulse" />
      ))}
    </div>
  );
}

export default function MyPostsPage() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<MyPostsFallback />}>
        <section className="p-12 pb-24">
          <MyPostsSection />
        </section>
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
