"use client";

import Link from "next/link";
import { MdPeople, MdArticle, MdComment, MdQuiz } from "react-icons/md";
import { useAdminDashboardStats } from "@/hooks/useAdmin";
import { ROUTES } from "@/constants/routes";

const STAT_CARDS = [
  {
    label: "총 유저 수",
    key: "totalUsers" as const,
    icon: MdPeople,
    href: ROUTES.adminUsers,
    iconClass: "text-blue-600 bg-blue-50 dark:bg-blue-950",
  },
  {
    label: "총 게시글 수",
    key: "totalPosts" as const,
    icon: MdArticle,
    href: ROUTES.adminPosts,
    iconClass: "text-emerald-600 bg-emerald-50 dark:bg-emerald-950",
  },
  {
    label: "총 댓글 수",
    key: "totalComments" as const,
    icon: MdComment,
    href: ROUTES.adminComments,
    iconClass: "text-violet-600 bg-violet-50 dark:bg-violet-950",
  },
];

const QUICK_LINKS = [
  { label: "유저 관리",     href: ROUTES.adminUsers,    icon: MdPeople },
  { label: "게시글 관리",   href: ROUTES.adminPosts,    icon: MdArticle },
  { label: "댓글 관리",     href: ROUTES.adminComments, icon: MdComment },
  { label: "MBTI 문제 관리", href: ROUTES.adminMbti,    icon: MdQuiz },
];

export default function AdminDashboardContent() {
  const stats = useAdminDashboardStats();

  return (
    <div className="p-6 max-w-5xl">
      <h1 className="text-2xl font-bold text-foreground mb-6">대시보드</h1>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
        {STAT_CARDS.map(({ label, key, icon: Icon, href, iconClass }) => (
          <Link
            key={href}
            href={href}
            className="rounded-2xl border border-outline-variant bg-card p-5 hover:bg-accent transition-colors flex items-center gap-4"
          >
            <div className={`rounded-xl p-3 ${iconClass}`}>
              <Icon size={24} />
            </div>
            <div>
              <p className="text-sm text-on-surface-variant">{label}</p>
              <p className="text-2xl font-bold text-foreground">
                {stats[key].toLocaleString()}
              </p>
            </div>
          </Link>
        ))}
      </div>

      <div className="rounded-2xl border border-outline-variant bg-card p-5">
        <h2 className="text-base font-semibold text-foreground mb-3">빠른 이동</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {QUICK_LINKS.map(({ label, href, icon: Icon }) => (
            <Link
              key={href}
              href={href}
              className="flex flex-col items-center gap-2 p-4 rounded-xl border border-outline-variant hover:bg-accent transition-colors text-center"
            >
              <Icon size={22} className="text-on-surface-variant" />
              <span className="text-sm text-on-surface-variant">{label}</span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
