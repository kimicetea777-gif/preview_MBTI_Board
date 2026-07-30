import { Suspense } from "react";
import type { Metadata } from "next";
import MbtiDetailContent from "./MbtiDetailContent";
import QueryResetErrorBoundary from "@/components/common/QueryResetErrorBoundary";
import LoadingIndicator from "@/components/common/LoadingIndicator";
import { getMbtiById } from "@/constants/mbtiData";
import { APP_NAME } from "@/constants/site";

export const dynamic = "force-static";

interface Props {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const mbti = getMbtiById(id);
  if (!mbti) return {};

  return {
    title: `${mbti.id} 유형 특징`,
    description: `${mbti.id} 유형: ${mbti.title}. ${APP_NAME}에서 ${mbti.id} 유형에 대해 자세히 알아보고 다른 사람들의 이야기도 확인해보세요.`,
  };
}

export default async function MbtiDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <QueryResetErrorBoundary>
      <Suspense fallback={<LoadingIndicator className="flex justify-center py-20 text-sm text-muted-foreground" />}>
        <MbtiDetailContent id={id} />
      </Suspense>
    </QueryResetErrorBoundary>
  );
}
