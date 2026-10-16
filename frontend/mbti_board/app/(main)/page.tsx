import { permanentRedirect } from "next/navigation";
import { ROUTES } from "@/constants/routes";

export const dynamic = "force-static";

export default function HomePage() {
  permanentRedirect(ROUTES.board.all); // 308 — 검색엔진이 /board를 대표 URL로 인식하도록 영구 리다이렉트
}
