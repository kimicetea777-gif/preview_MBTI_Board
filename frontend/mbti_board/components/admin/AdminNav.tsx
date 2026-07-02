"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { IconType } from "react-icons";
import { MdClose, MdArrowBack } from "react-icons/md";
import {
  ADMIN_DASHBOARD_TABS, ADMIN_USER_TABS,
  ADMIN_POST_TABS, ADMIN_COMMENT_TABS, ADMIN_MBTI_TABS, ADMIN_ALERT_TABS, ADMIN_NOTICE_TABS,
} from "@/constants/navigation";
import { ROUTES } from "@/constants/routes";
import { useNavStore } from "@/store/navStore";
import Logo from "@/components/common/Logo";

export default function AdminNav() {
  const pathname = usePathname();
  const { drawerOpen, closeDrawer } = useNavStore();

  return (
    <>
      {/* 데스크탑 사이드바 */}
      <aside className="hidden md:flex flex-col gap-3 w-57 shrink-0 sticky top-16 self-start py-6 px-4">
        <div className="rounded-2xl border border-outline-variant bg-card px-3 py-4">
          <nav className="flex flex-col">
            <NavContent pathname={pathname} />
          </nav>
          <div className="h-px bg-outline-variant my-3" />
          <Link
            href={ROUTES.board.all}
            className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-accent hover:text-foreground transition-colors"
          >
            <MdArrowBack size={20} />
            <span>일반 사이트로</span>
          </Link>
        </div>
      </aside>

      {/* 모바일 드로어 */}
      {drawerOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/40" onClick={closeDrawer} />
          <div className="fixed inset-y-0 left-0 z-50 w-64 bg-background shadow-xl flex flex-col rounded-r-3xl">
            <div className="flex items-center justify-between px-4 h-16 shrink-0">
              <Logo size="md" />
              <button
                onClick={closeDrawer}
                className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
              >
                <MdClose size={22} />
              </button>
            </div>
            <nav className="flex flex-col p-3 overflow-y-auto">
              <p className="px-3 py-1 text-xs font-semibold text-on-surface-variant uppercase tracking-wide mb-1">
                관리자 메뉴
              </p>
              <NavContent pathname={pathname} onNavClick={closeDrawer} />
              <div className="h-px bg-outline-variant my-3" />
              <Link
                href={ROUTES.board.all}
                onClick={closeDrawer}
                className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-on-surface-variant hover:bg-accent hover:text-foreground transition-colors"
              >
                <MdArrowBack size={20} />
                <span>일반 사이트로</span>
              </Link>
            </nav>
          </div>
        </>
      )}
    </>
  );
}

const ADMIN_NAV_GROUPS = [
  { label: null, tabs: ADMIN_DASHBOARD_TABS },
  { label: null, tabs: ADMIN_USER_TABS },
  { label: null, tabs: ADMIN_POST_TABS },
  { label: null, tabs: ADMIN_COMMENT_TABS },
  { label: null, tabs: ADMIN_MBTI_TABS },
  { label: null, tabs: ADMIN_NOTICE_TABS },
  { label: null, tabs: ADMIN_ALERT_TABS },
] as const;

function NavContent({ pathname, onNavClick }: { pathname: string; onNavClick?: () => void }) {
  return (
    <>
      {ADMIN_NAV_GROUPS.map(({ label, tabs }, i) => (
        <div key={tabs[0].href}>
          {i !== 0 && <div className="h-px bg-outline-variant my-1.5" />}
          {label && (
            <p className="px-3 pt-1 pb-0.5 text-xs font-semibold text-on-surface-variant uppercase tracking-wide">
              {label}
            </p>
          )}
          {tabs.map(({ label: tabLabel, href, icon }) => (
            <NavItem
              key={href}
              href={href}
              icon={icon}
              label={tabLabel}
              active={pathname === href}
              onClick={onNavClick}
            />
          ))}
        </div>
      ))}
    </>
  );
}

function NavItem({ href, icon: Icon, label, active, onClick }: {
  href: string;
  icon: IconType;
  label: string;
  active: boolean;
  onClick?: () => void;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
        active
          ? "bg-sidebar-primary/85 font-semibold text-sidebar"
          : "text-on-surface-variant hover:bg-accent hover:text-foreground"
      }`}
    >
      <Icon size={20} />
      <span>{label}</span>
    </Link>
  );
}
