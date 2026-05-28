import WriteFormContent from "./WriteFormContent";
import type { PostType } from "@/types/post";

function parseInitialType(param: string | undefined): PostType {
  if (param === "VOTE" || param === "DEBATE") return param;
  return "FREE";
}

export default function UnifiedWritePage({
  searchParams,
}: {
  searchParams: { type?: string };
}) {
  return <WriteFormContent initialType={parseInitialType(searchParams.type)} />;
}
