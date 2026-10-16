import type { MetadataRoute } from "next";
import { ROUTES } from "@/constants/routes";
import { POST_TYPE, type PostSitemapEntry, type PostType } from "@/types/post";

export const revalidate = 3600; // 1시간마다 재생성 — 매 크롤링 요청마다 백엔드 페이지네이션 전체 조회하지 않도록

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
const BACKEND_URL = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";
const PAGE_SIZE = 100;

interface ApiPage<T> {
  content: T[];
  totalPages: number;
}

async function fetchAllPosts(type: PostType): Promise<PostSitemapEntry[]> {
  const posts: PostSitemapEntry[] = [];
  let page = 0;
  let totalPages = 1;

  try {
    while (page < totalPages) {
      const res = await fetch(
        `${BACKEND_URL}/api/posts/sitemap-entries?type=${type}&size=${PAGE_SIZE}&page=${page}`,
        { next: { revalidate } },
      );
      if (!res.ok) {
        console.error(`sitemap: failed to fetch ${type} page ${page} (${res.status})`);
        break;
      }

      const { data }: { data: ApiPage<PostSitemapEntry> } = await res.json();
      posts.push(...data.content);
      totalPages = data.totalPages;
      page += 1;
    }
  } catch (err) {
    // 백엔드 연결 실패 등 — sitemap.xml 전체가 죽지 않도록 이 타입만 빈 목록으로 넘어감
    console.error(`sitemap: error fetching ${type} posts`, err);
  }

  return posts;
}

const PATH_BY_TYPE: Record<PostType, (id: number) => string> = {
  FREE: ROUTES.board.freePost,
  VOTE: ROUTES.board.votePost,
  DEBATE: ROUTES.board.debatePost,
};

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: `${SITE_URL}${ROUTES.board.all}`, changeFrequency: "hourly", priority: 1 }, // "/"는 /board로 리다이렉트되므로 sitemap에서 제외
    { url: `${SITE_URL}${ROUTES.mbti.list}`, changeFrequency: "weekly", priority: 0.7 },
    { url: `${SITE_URL}${ROUTES.mbti.test}`, changeFrequency: "monthly", priority: 0.6 },
    { url: `${SITE_URL}${ROUTES.terms}`, changeFrequency: "yearly", priority: 0.2 },
    { url: `${SITE_URL}${ROUTES.privacy}`, changeFrequency: "yearly", priority: 0.2 },
  ];

  const postTypes: PostType[] = [POST_TYPE.FREE, POST_TYPE.VOTE, POST_TYPE.DEBATE];
  const postsByType = await Promise.all(postTypes.map(fetchAllPosts));

  const postRoutes: MetadataRoute.Sitemap = postTypes.flatMap((type, i) =>
    postsByType[i].map((post) => ({
      url: `${SITE_URL}${PATH_BY_TYPE[type](post.id)}`,
      lastModified: post.createdAt,
      changeFrequency: "weekly" as const,
      priority: 0.5,
    })),
  );

  return [...staticRoutes, ...postRoutes];
}
