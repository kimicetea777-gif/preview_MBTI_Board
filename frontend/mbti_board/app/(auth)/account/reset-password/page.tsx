import { redirect } from "next/navigation";
import ResetPasswordContent from "@/components/auth/ResetPasswordContent";
import { ROUTES } from "@/constants/routes";

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  if (!token) redirect(ROUTES.accountFind);
  return <ResetPasswordContent token={token} />;
}
