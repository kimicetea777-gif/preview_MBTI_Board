import RecoveryEmailVerifyContent from "@/components/my/account/RecoveryEmailVerifyContent";

export default async function RecoveryEmailVerifyPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  return <RecoveryEmailVerifyContent token={token ?? null} />;
}
