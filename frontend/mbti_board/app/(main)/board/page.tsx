import type { Metadata } from "next";
import BoardPageClient from "./BoardPageClient";

export const dynamic = "force-dynamic";

// 탭 필터(?type=FREE 등)는 같은 게시판의 부분집합이므로 기본 경로로 canonical 고정
export const metadata: Metadata = {
  alternates: { canonical: "/board" },
};

export default function BoardPage() {
  return <BoardPageClient />;
}
