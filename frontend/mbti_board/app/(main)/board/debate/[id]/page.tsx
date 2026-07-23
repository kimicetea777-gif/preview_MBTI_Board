import { Suspense } from "react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import DebateDetailContent from "./DebateDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import PostDetailSkeleton from "@/components/post/PostDetailSkeleton";
import { buildPostMetadata } from "@/lib/postMetadata";
import { ROUTES } from "@/constants/routes";

export const dynamic = "force-dynamic";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}): Promise<Metadata> {
  const { id } = await params;
  return buildPostMetadata(Number(id), ROUTES.board.debatePost);
}

export default async function DebatePostDetailPage({
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
        <DebateDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
