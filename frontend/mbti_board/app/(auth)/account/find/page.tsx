import FindAccountContent from "@/components/auth/FindAccountContent";

export default async function FindAccountPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>;
}) {
  const { tab } = await searchParams;
  const initialTab = tab === "password" ? "password" : "email";
  return <FindAccountContent initialTab={initialTab} />;
}
