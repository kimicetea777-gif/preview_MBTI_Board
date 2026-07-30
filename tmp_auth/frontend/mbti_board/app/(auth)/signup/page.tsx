import SignupContent from "@/components/auth/SignupContent";
import { STEP } from "@/app/(auth)/signup/steps";

export default async function SignupPage({
  searchParams,
}: {
  searchParams: Promise<{ step?: string; email?: string }>;
}) {
  const { step, email } = await searchParams;
  const isFormStep = step === STEP.FORM && !!email;

  return (
    <SignupContent
      initialStep={isFormStep ? STEP.FORM : STEP.EMAIL}
      initialEmail={isFormStep ? email! : ""}
    />
  );
}
