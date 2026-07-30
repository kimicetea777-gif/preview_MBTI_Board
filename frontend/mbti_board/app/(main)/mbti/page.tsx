import type { Metadata } from "next";
import MbtiListContent from "./MbtiListContent";

export const metadata: Metadata = {
  title: "MBTI 16가지 성격 유형",
  description: "MBTI 16가지 성격 유형을 한눈에 살펴보세요. 각 유형의 특징과 잘 맞는 유형, 안 맞는 유형까지 확인할 수 있습니다.",
};

export const dynamic = "force-static";

export default function MbtiListPage() {
  // API X, 로컬에서 mbti 데이터 가져오
  return <MbtiListContent />;
}
