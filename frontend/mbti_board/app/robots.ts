import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [
        "/my",
        "/admin",
        "/login",
        "/signup",
        "/account",
        "/oauth2",
        "/board/write",
        "/terms/agree",
      ],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
