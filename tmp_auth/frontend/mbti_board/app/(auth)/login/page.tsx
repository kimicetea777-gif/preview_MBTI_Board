import LoginForm from "@/components/auth/LoginForm";
import { ROUTES } from "@/constants/routes";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ redirect?: string }>;
}) {
  const { redirect } = await searchParams;
  return <LoginForm redirectTo={redirect ?? ROUTES.home} />;
}
