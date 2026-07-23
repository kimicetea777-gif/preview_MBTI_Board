import { Suspense } from "react";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import MbtiTestContent from "./MbtiTestContent";
import MbtiTestLoading from "./loading";

export const dynamic = "force-dynamic";

export default function MbtiTestPage() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<MbtiTestLoading />}>
        <MbtiTestContent />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
