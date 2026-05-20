import OAuth2CallbackContent from "@/components/auth/OAuth2CallbackContent";

export default async function OAuth2CallbackPage({
  searchParams,
}: {
  searchParams: Promise<{ accessToken?: string; error?: string }>;
}) {
  const { accessToken, error } = await searchParams;
  return <OAuth2CallbackContent accessToken={accessToken ?? null} error={error ?? null} />;
}
