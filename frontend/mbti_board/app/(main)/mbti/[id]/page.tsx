import { Suspense } from "react";
import MbtiDetailContent from "./MbtiDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function MbtiDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<div className="flex justify-center py-20 text-sm text-muted-foreground">불러오는 중...</div>}>
        <MbtiDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
