"use client";

import { useState, useEffect } from "react";
import { useMutation } from "@tanstack/react-query";
import { sendEmailVerification, signup } from "@/lib/api/auth";

import { STEP, type Step } from "@/app/(auth)/signup/steps";
import { getApiErrorMessage, getApiErrorStatus } from "@/lib/apiError";

import EmailStep from "@/components/auth/steps/EmailStep";
import EmailSentStep from "@/components/auth/steps/EmailSentStep";
import FormStep from "@/components/auth/steps/FormStep";
import DoneStep from "@/components/auth/steps/DoneStep";

const EV_EMAIL_KEY = "ev_email";

interface Props {
  initialStep: Step;
  initialEmail: string;
}

export default function SignupContent({ initialStep, initialEmail }: Props) {
  const [step, setStep] = useState<Step>(initialStep);
  const [email, setEmail] = useState(initialEmail);
  const [countdown, setCountdown] = useState(0);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown(c => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  const sendMutation = useMutation({
    mutationFn: sendEmailVerification,
    onSuccess: () => {
      sessionStorage.setItem(EV_EMAIL_KEY, email);
      setStep(STEP.EMAIL_SENT);
      setCountdown(60);
    },
  });

  const signupMutation = useMutation({
    mutationFn: signup,
    onSuccess: () => setStep(STEP.DONE),
  });

  const sendErrorMsg = sendMutation.isError
    ? getApiErrorMessage(sendMutation.error, "이메일 발송에 실패했습니다. 다시 시도해 주세요.")
    : null;

  const signupApiErrorMsg = signupMutation.isError
    ? getApiErrorMessage(signupMutation.error, "회원가입에 실패했습니다. 이메일 인증을 완료했는지 확인해 주세요.")
    : null;

  const is409 = getApiErrorStatus(signupMutation.error) === 409;
  const isDuplicateNickname = is409 && !!signupApiErrorMsg?.includes("닉네임");
  const isDuplicatePhone = is409 && !!signupApiErrorMsg?.includes("전화번호");
  const hasAuthError = getApiErrorStatus(signupMutation.error) === 400;

  const goToEmail = () => {
    setStep(STEP.EMAIL);
    sendMutation.reset();
    signupMutation.reset();
  };

  if (step === STEP.EMAIL) {
    return (
      <EmailStep
        email={email}
        onEmailChange={setEmail}
        onSubmit={() => sendMutation.mutate({ email })}
        isPending={sendMutation.isPending}
        errorMsg={sendErrorMsg}
      />
    );
  }

  if (step === STEP.EMAIL_SENT) {
    return (
      <EmailSentStep
        email={email}
        countdown={countdown}
        isPending={sendMutation.isPending}
        onResend={() => sendMutation.mutate({ email })}
        onBack={() => { setStep(STEP.EMAIL); sendMutation.reset(); }}
      />
    );
  }

  if (step === STEP.FORM) {
    return (
      <FormStep
        email={email}
        onReauth={goToEmail}
        hasAuthError={hasAuthError}
        onSubmit={(data) => signupMutation.mutate(data)}
        isPending={signupMutation.isPending}
        apiErrorMsg={signupApiErrorMsg}
        isDuplicateNickname={isDuplicateNickname}
        isDuplicatePhone={isDuplicatePhone}
      />
    );
  }

  return <DoneStep />;
}
