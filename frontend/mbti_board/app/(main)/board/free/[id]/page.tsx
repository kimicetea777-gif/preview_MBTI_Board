import { Suspense } from "react";
import { notFound } from "next/navigation";
import PostDetailContent from "./PostDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import PostDetailSkeleton from "@/components/post/PostDetailSkeleton";

export default async function PostDetailPage({
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
        <PostDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
