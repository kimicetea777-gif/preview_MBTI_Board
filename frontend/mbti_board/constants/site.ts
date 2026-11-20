export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
export const AUTH_SERVER_URL = process.env.NEXT_PUBLIC_AUTH_SERVER_URL ?? "http://localhost:9000";
export const APP_NAME = process.env.NEXT_PUBLIC_APP_NAME ?? "TheBlueButter";
export const SITE_DESCRIPTION ="MBTI 기반 커뮤니티 | 1.무료 MBTI검사 2.MBTI 커뮤니티. 같은 유형, 다른 유형 사람들에게 질문하세요.";
export const CONTACT_EMAIL = "kimicetea777@gmail.com";
export const CONTACT_MAILTO = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(`[${APP_NAME}] 문의`)}`;
export const SENDER_EMAIL = process.env.NEXT_PUBLIC_SENDER_EMAIL;

/**
 * sitemap.xml에 게시글 URL을 넣을지 여부. false면 `app/sitemap.ts`가 백엔드 `/api/posts/sitemap-entries`를 호출하지 않고 정적 경로만 낸다.
 * 사용자·글이 적은 동안 꺼 둔다. 켜면 검색엔진이 sitemap으로 글을 발견한다 (다시 빌드해야 반영된다).
 */
export const SITEMAP_INCLUDE_POSTS = false;
