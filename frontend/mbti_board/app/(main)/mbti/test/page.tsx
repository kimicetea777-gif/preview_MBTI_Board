import { Suspense } from "react";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import MbtiTestContent from "./MbtiTestContent";
import MbtiTestLoading from "./loading";

export default function MbtiTestPage() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<MbtiTestLoading />}>
        <MbtiTestContent />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
