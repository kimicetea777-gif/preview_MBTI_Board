import { notFound } from "next/navigation";
import VoteDetailContent from "./VoteDetailContent";

export default async function VotePostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idParam } = await params;
  const id = Number(idParam);
  if (!Number.isInteger(id) || id <= 0) notFound();

  return <VoteDetailContent id={id} />;
}
