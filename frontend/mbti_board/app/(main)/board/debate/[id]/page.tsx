import { notFound } from "next/navigation";
import DebateDetailContent from "./DebateDetailContent";

export default async function DebatePostDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idParam } = await params;
  const id = Number(idParam);
  if (!Number.isInteger(id) || id <= 0) notFound();

  return <DebateDetailContent id={id} />;
}
