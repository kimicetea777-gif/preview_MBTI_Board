"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import type { IconType } from "react-icons";
import {
  MdHome, MdChatBubbleOutline, MdHowToVote, MdForum,
  MdPerson, MdArticle, MdPsychology, MdQuiz,
} from "react-icons/md";
import { ROUTES } from "@/lib/routes";

const BOARD_TABS: { label: string; href: string; type: string | null; icon: IconType }[] = [
  { label: "전체",        href: ROUTES.board.all,    type: null,     icon: MdHome },
  { label: "자유 게시판", href: ROUTES.board.free,   type: "FREE",   icon: MdChatBubbleOutline },
  { label: "투표 게시판", href: ROUTES.board.vote,   type: "VOTE",   icon: MdHowToVote },
  { label: "토론 게시판", href: ROUTES.board.debate, type: "DEBATE", icon: MdForum },
];

const MY_TABS: { label: string; href: string; icon: IconType }[] = [
  { label: "프로필",     href: ROUTES.my,      icon: MdPerson },
  { label: "내 게시글", href: ROUTES.myPosts, icon: MdArticle },
];

const MBTI_TABS: { label: string; href: string; icon: IconType }[] = [
  { label: "MBTI 유형", href: ROUTES.mbti.list, icon: MdPsychology },
  { label: "MBTI 검사", href: ROUTES.mbti.test, icon: MdQuiz },
];

export default function Sidebar() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentType = searchParams.get("type");

  const isMyPage = pathname.startsWith(ROUTES.my);
  const isMbtiPage = pathname.startsWith(ROUTES.mbti.list);

  if (isMyPage) {
    return (
      <SidebarShell label="마이페이지">
        {MY_TABS.map(({ label, href, icon }) => (
          <NavItem
            key={href}
            href={href}
            icon={icon}
            label={label}
            active={pathname === href || pathname.startsWith(href + "/")}
          />
        ))}
      </SidebarShell>
    );
  }

  if (isMbtiPage) {
    return (
      <SidebarShell label="MBTI">
        {MBTI_TABS.map(({ label, href, icon }) => (
          <NavItem
            key={href}
            href={href}
            icon={icon}
            label={label}
            active={pathname === href || pathname.startsWith(href + "/")}
          />
        ))}
      </SidebarShell>
    );
  }

  return (
    <SidebarShell label="게시판">
      {BOARD_TABS.map(({ label, href, type, icon }) => {
        const active = pathname === "/board" && currentType === type;
        return (
          <NavItem key={href} href={href} icon={icon} label={label} active={active} />
        );
      })}
      <div className="h-px bg-outline-variant my-3" />
      <NavItem href={ROUTES.mbti.test} icon={MdQuiz} label="MBTI 검사" active={false} />
      <NavItem href={ROUTES.mbti.list} icon={MdPsychology} label="MBTI 유형" active={false} />
    </SidebarShell>
  );
}

function SidebarShell({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <aside className="hidden md:flex flex-col gap-1 py-6 px-4 w-64 shrink-0 bg-surface-container-low border-r border-outline-variant">
      <p className="mb-3 px-3 text-xs font-semibold uppercase tracking-wider text-on-surface-variant opacity-60">
        {label}
      </p>
      <nav className="flex flex-col gap-0.5">
        {children}
      </nav>
    </aside>
  );
}

function NavItem({ href, icon: Icon, label, active }: {
  href: string;
  icon: IconType;
  label: string;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
        active
          ? "bg-secondary-container text-on-secondary-container font-semibold"
          : "text-on-surface-variant hover:bg-accent hover:text-foreground"
      }`}
    >
      <Icon size={20} />
      <span>{label}</span>
    </Link>
  );
}
