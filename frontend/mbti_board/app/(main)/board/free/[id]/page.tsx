import { notFound } from "next/navigation";
import PostDetailContent from "./PostDetailContent";

export default async function PostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idParam } = await params;
  const id = Number(idParam);
  if (!Number.isInteger(id) || id <= 0) notFound();

  return <PostDetailContent id={id} />;
}
