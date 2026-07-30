import FindAccountContent from "@/components/auth/FindAccountContent";
import { FIND_ACCOUNT_TAB } from "@/types/auth";

export default async function FindAccountPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>;
}) {
  const { tab } = await searchParams;
  const initialTab = tab === FIND_ACCOUNT_TAB.PASSWORD
    ? FIND_ACCOUNT_TAB.PASSWORD
    : FIND_ACCOUNT_TAB.EMAIL;
  return <FindAccountContent initialTab={initialTab} />;
}
