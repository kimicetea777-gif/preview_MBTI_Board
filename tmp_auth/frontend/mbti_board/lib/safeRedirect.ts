/**
 * 외부 URL 및 프로토콜 상대 URL(//evil.com)로의 Open Redirect를 차단.
 * 내부 경로("/board/free" 등)만 허용하고 그 외는 "/"로 폴백.
 */
export function toSafeRedirect(redirectTo: string): string {
  return redirectTo.startsWith("/") && !redirectTo.startsWith("//") 
    ? redirectTo 
    : "/";
}
