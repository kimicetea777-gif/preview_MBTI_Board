import { NextRequest, NextResponse } from "next/server";
import { ROUTES } from "@/lib/routes";

// session 쿠키(프론트 도메인에 설정)로 판단. 실제 유효성 검증은 API 서버에서 수행.
// RT(HttpOnly, localhost:8080)는 Next.js 미들웨어에서 읽을 수 없어 별도 힌트 쿠키 사용.
const PROTECTED_PREFIXES = [
  ROUTES.my,
  ROUTES.board.freeWrite,
  ROUTES.board.voteWrite,
  ROUTES.board.debateWrite,
];

export function middleware(request: NextRequest) {
  const session = request.cookies.get("session");
  const { pathname } = request.nextUrl;

  const isProtected = PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));

  if (isProtected && !session) {
    const loginUrl = new URL(ROUTES.login, request.url);
    loginUrl.searchParams.set("redirect", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/my/:path*", "/board/:path*/write"],
};
