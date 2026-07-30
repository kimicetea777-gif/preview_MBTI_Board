import { Suspense } from "react";
import type { Metadata } from "next";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import MbtiTestContent from "./MbtiTestContent";
import MbtiTestLoading from "./loading";
import { APP_NAME } from "@/constants/site";

export const metadata: Metadata = {
  title: "무료 MBTI 검사",
  description: `${APP_NAME}에서 제공하는 무료 MBTI 성격 유형 검사 — 16가지 유형 중 나는 어떤 유형일까? 검사 후 같은 유형, 다른 유형 사람들의 생생한 글과 의견도 함께 확인해보세요.`,
};

export const dynamic = "force-dynamic";

export default function MbtiTestPage() {
  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<MbtiTestLoading />}>
        <MbtiTestContent />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
