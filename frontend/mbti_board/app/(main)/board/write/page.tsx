import WriteFormContent from "./WriteFormContent";
import type { PostType } from "@/types/post";

function parseInitialType(param: string | undefined): PostType {
  if (param === "VOTE" || param === "DEBATE") return param;
  return "FREE";
}

export default async function UnifiedWritePage({
  searchParams,
}: {
  searchParams: Promise<{ type?: string }>;
}) {
  const { type } = await searchParams;
  return <WriteFormContent initialType={parseInitialType(type)} />;
}
