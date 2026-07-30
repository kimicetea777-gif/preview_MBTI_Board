import OAuth2CallbackContent from "@/components/auth/OAuth2CallbackContent";

export default async function OAuth2CallbackPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const { error } = await searchParams;
  return <OAuth2CallbackContent error={error ?? null} />;
}
