import ResetPasswordContent from "@/components/auth/ResetPasswordContent";

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  return <ResetPasswordContent token={token ?? null} />;
}
