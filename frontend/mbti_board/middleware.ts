import { NextRequest, NextResponse } from "next/server";
import { ROUTES } from "@/lib/routes";

// RT(HttpOnly Cookie) 존재 여부로만 판단. 실제 유효성 검증은 API 서버에서 수행.
const PROTECTED_PREFIXES = [
  ROUTES.my,
  ROUTES.board.freeWrite,
  ROUTES.board.voteWrite,
  ROUTES.board.debateWrite,
];

export function middleware(request: NextRequest) {
  const rt = request.cookies.get("refreshToken");
  const { pathname } = request.nextUrl;

  const isProtected = PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));

  if (isProtected && !rt) {
    const loginUrl = new URL(ROUTES.login, request.url);
    loginUrl.searchParams.set("redirect", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/my/:path*", "/board/:path*/write"],
};
