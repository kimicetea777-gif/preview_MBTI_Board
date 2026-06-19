import { redirect } from "next/navigation";
import { ROUTES } from "@/constants/routes";

export default function DebateBoardWriteRedirect() {
  redirect(ROUTES.board.debateWrite);
}
