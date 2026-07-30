import SignupVerifyContent from "@/components/auth/SignupVerifyContent";

export default async function SignupVerifyPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  return <SignupVerifyContent token={token ?? null} />;
}
