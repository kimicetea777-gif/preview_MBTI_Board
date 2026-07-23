import WriteFormContent from "./WriteFormContent";
import { POST_TYPE, type PostType } from "@/types/post";

export const dynamic = "force-dynamic";

function parseInitialType(param: string | undefined): PostType {
  if (param === POST_TYPE.VOTE || param === POST_TYPE.DEBATE) return param;
  return POST_TYPE.FREE;
}

export default async function UnifiedWritePage({
  searchParams,
}: {
  searchParams: Promise<{ type?: string }>;
}) {
  const { type } = await searchParams;
  return <WriteFormContent initialType={parseInitialType(type)} />;
}
