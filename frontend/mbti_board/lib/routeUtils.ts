import { ROUTES } from "@/constants/routes";

export function isMyPage(pathname: string): boolean {
  return pathname.startsWith(ROUTES.my);
}
