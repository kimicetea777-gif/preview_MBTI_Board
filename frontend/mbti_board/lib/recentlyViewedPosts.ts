import { POST_TYPE, type PostType } from "@/types/post";

const STORAGE_KEY = "recently_viewed_posts";
const MAX_ITEMS = 5;

export interface RecentlyViewedPost {
  id: number;
  typeCode: PostType;
  title: string;
  viewedAt: string;
}

const VALID_TYPES = new Set<string>(Object.values(POST_TYPE));

function isValid(p: unknown): p is RecentlyViewedPost {
  return (
    typeof p === "object" &&
    p !== null &&
    typeof (p as RecentlyViewedPost).id === "number" &&
    VALID_TYPES.has((p as RecentlyViewedPost).typeCode) &&
    typeof (p as RecentlyViewedPost).title === "string" &&
    typeof (p as RecentlyViewedPost).viewedAt === "string"
  );
}

let cachedRaw: string | null = null;
let cachedSnapshot: RecentlyViewedPost[] = [];

export function recordRecentlyViewed(post: Omit<RecentlyViewedPost, "viewedAt">): void {
  const current = getRecentlyViewed();
  const filtered = current.filter((p) => p.id !== post.id);
  const next: RecentlyViewedPost[] = [
    { ...post, viewedAt: new Date().toISOString() },
    ...filtered,
  ].slice(0, MAX_ITEMS);
  cachedRaw = JSON.stringify(next);
  cachedSnapshot = next;
  try {
    localStorage.setItem(STORAGE_KEY, cachedRaw);
  } catch {
    // 저장 실패는 무시 (사파리 프라이빗 모드, 저장공간 초과 등) — 부가 기능이라 크래시로 이어지면 안 됨
  }
}

export function getRecentlyViewed(): RecentlyViewedPost[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw === cachedRaw) return cachedSnapshot;
    cachedRaw = raw;
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    cachedSnapshot = Array.isArray(parsed) ? parsed.filter(isValid) : [];
    return cachedSnapshot;
  } catch {
    return cachedSnapshot;
  }
}
