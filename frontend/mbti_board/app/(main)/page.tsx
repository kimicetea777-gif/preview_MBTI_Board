import { redirect } from "next/navigation";
import { ROUTES } from "@/constants/routes";

export const dynamic = "force-static";

export default function HomePage() {
  redirect(ROUTES.board.all);
}
