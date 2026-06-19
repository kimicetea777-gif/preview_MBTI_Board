import { Suspense } from "react";
import { notFound } from "next/navigation";
import VoteDetailContent from "./VoteDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import PostDetailSkeleton from "@/components/post/PostDetailSkeleton";

export default async function VotePostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idParam } = await params;
  const id = Number(idParam);
  if (!Number.isInteger(id) || id <= 0) notFound();

  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<PostDetailSkeleton />}>
        <VoteDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
