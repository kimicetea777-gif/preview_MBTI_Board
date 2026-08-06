import { NextRequest, NextResponse } from "next/server";
import { ROUTES } from "@/constants/routes";

// session 쿠키(프론트 도메인에 설정)로 판단. 실제 유효성 검증은 API 서버에서 수행.
// FREE 게시글 작성은 비회원 허용. /board/write?type=VOTE|DEBATE 만 로그인 필요.
// /admin 은 로그인 필수 (ADMIN 권한 검증은 API 서버에서 수행, 미권한 시 페이지에서 403 처리).
function isProtected(request: NextRequest): boolean {
  const { pathname, searchParams } = request.nextUrl;

  if (pathname.startsWith(ROUTES.my)) return true;
  if (pathname.startsWith(ROUTES.admin)) return true;

  if (pathname === "/board/write") {
    const type = searchParams.get("type");
    return type === "VOTE" || type === "DEBATE";
  }

  return false;
}

export function proxy(request: NextRequest) {
  const session = request.cookies.get("session");

  if (isProtected(request) && !session) {
    const { pathname, search } = request.nextUrl;
    const loginUrl = new URL(ROUTES.login, request.url);
    loginUrl.searchParams.set("redirect", pathname + search);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/my/:path*", "/admin/:path*", "/board/write"],
};
