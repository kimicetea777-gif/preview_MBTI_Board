import { Suspense } from "react";
import MbtiDetailContent from "./MbtiDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";

export const dynamic = "force-static";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function MbtiDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<LoadingIndicator className="flex justify-center py-20 text-sm text-muted-foreground" />}>
        <MbtiDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
