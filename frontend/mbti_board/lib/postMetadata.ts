import type { Metadata } from "next";
import type { PostMetaResponse } from "@/types/post";
import { APP_NAME } from "@/constants/site";

const BACKEND_URL = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";
const DEFAULT_OG_IMAGE = "/images/og.png"; // 게시글 자체 이미지가 없을 때 공유 미리보기용 폴백 배너

async function fetchPostMeta(id: number): Promise<PostMetaResponse | null> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/posts/${id}/meta`, { next: { revalidate: 60 } });
    if (!res.ok) return null;
    const { data }: { data: PostMetaResponse } = await res.json();
    return data;
  } catch {
    return null;
  }
}

/** 게시글 상세 페이지용 metadata 생성 — title/description/OG를 게시글 내용 기반으로 채움 */
export async function buildPostMetadata(id: number, path: (id: number) => string): Promise<Metadata> {
  const post = await fetchPostMeta(id);
  if (!post) return {};

  // 이미지만 있는 글 등 미리보기가 비면 description 태그가 아예 빠지므로 제목 기반 문구로 대체
  const description = post.contentPreview || `${post.title} — ${APP_NAME} MBTI 커뮤니티 게시글`;
  const image = post.imageUrls?.[0] ?? DEFAULT_OG_IMAGE;

  return {
    title: post.title,
    description,
    alternates: { canonical: path(id) },
    openGraph: {
      type: "article",
      title: post.title,
      description,
      url: path(id),
      images: [{ url: image, alt: post.title }],
    },
    twitter: {
      card: "summary_large_image",
      title: post.title,
      description,
      images: [{ url: image, alt: post.title }],
    },
  };
}
