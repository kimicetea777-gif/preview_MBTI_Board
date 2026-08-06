export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
export const AUTH_SERVER_URL = process.env.NEXT_PUBLIC_AUTH_SERVER_URL ?? "http://localhost:9000";
export const APP_NAME = process.env.NEXT_PUBLIC_APP_NAME ?? "TheBlueButter";
export const SITE_DESCRIPTION ="MBTI 기반 커뮤니티 | 1.무료 MBTI검사 2.MBTI 커뮤니티. 같은 유형, 다른 유형 사람들에게 질문하세요.";
export const CONTACT_EMAIL = "kimicetea777@gmail.com";
export const CONTACT_MAILTO = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(`[${APP_NAME}] 문의`)}`;
export const SENDER_EMAIL = process.env.NEXT_PUBLIC_SENDER_EMAIL;
