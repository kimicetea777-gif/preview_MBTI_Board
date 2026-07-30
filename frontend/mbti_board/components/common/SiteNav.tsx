"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import type { IconType } from "react-icons";
import { MdClose, MdChevronLeft, MdChevronRight } from "react-icons/md";
import { ROUTES } from "@/constants/routes";
import { CONTACT_MAILTO } from "@/constants/site";
import { BOARD_TABS, MY_TABS, EXTRA_TABS, STATS_TABS } from "@/constants/navigation";
import { useAuthStore } from "@/store/authStore";
import AdSenseCard from "./AdSenseCard";
import { AD_SLOTS, AD_VARIANT, AD_ENABLED } from "@/constants/adSlots";
import { useNavStore, getStoredSidebarCollapsed } from "@/store/navStore";
import { useIsDesktop } from "@/hooks/useIsDesktop";
import { useHasMounted } from "@/hooks/useHasMounted";
import Logo from "./Logo";

export default function SiteNav() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentType = searchParams.get("type");
  const { drawerOpen, closeDrawer, sidebarCollapsed, setSidebarCollapsed, toggleSidebarCollapsed } = useNavStore();

  const isDesktop = useIsDesktop();

  useEffect(() => {
    if (!drawerOpen) return;
    const original = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = original;
    };
  }, [drawerOpen]);

  // 서버 렌더·첫 클라이언트 렌더는 항상 펼침 상태로 고정 — 마운트 이후 localStorage 값으로 동기화 (hydration mismatch 방지)
  useEffect(() => {
    setSidebarCollapsed(getStoredSidebarCollapsed());
  }, [setSidebarCollapsed]);

  return (
    <>
      {/* 데스크탑 사이드바 */}
      <aside className={`hidden md:flex flex-col gap-3 shrink-0 sticky top-0 h-screen self-start overflow-y-auto py-6 px-4 transition-[width] duration-200 ${sidebarCollapsed ? "w-24" : "w-57"}`}>
        <div className={`flex items-center gap-2 h-10 shrink-0 ${sidebarCollapsed ? "justify-center" : "px-1"}`}>
          <button
            onClick={toggleSidebarCollapsed}
            className="p-1.5 rounded-full border border-outline-variant bg-card text-on-surface-variant shadow-sm hover:bg-accent transition-colors shrink-0"
            aria-label={sidebarCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
            title={sidebarCollapsed ? "펼치기" : "접기"}
          >
            {sidebarCollapsed ? <MdChevronRight size={18} /> : <MdChevronLeft size={18} />}
          </button>
          {!sidebarCollapsed && <Logo size="md" />}
        </div>
        <div className="rounded-2xl border border-outline-variant bg-card px-3 py-4">
          <nav className="flex flex-col">
            <NavContent pathname={pathname} currentType={currentType} collapsed={sidebarCollapsed} />
          </nav>
        </div>
        {AD_ENABLED && isDesktop && !sidebarCollapsed && <AdSenseCard key={AD_SLOTS.vertical.key} variant={AD_VARIANT.VERTICAL} />}
      </aside>

      {/* 모바일 드로어 */}
      {drawerOpen && (
        <>
          <div className="fixed inset-0 z-60 bg-black/40" onClick={closeDrawer} />
          <div className="fixed top-3 left-3 bottom-1 z-70 w-50 max-h-[calc(100vh-2rem)] bg-background shadow-xl flex flex-col rounded-3xl overflow-hidden">
            <div className="flex items-center justify-between px-4 h-16 border-outline-variant shrink-0">
              <Logo size="md"/>
              <button
                onClick={closeDrawer}
                className="p-2 rounded-full text-on-surface-variant hover:bg-accent transition-colors"
              >
                <MdClose size={22} />
              </button>
            </div>
            <nav className="flex flex-col p-3 overflow-y-auto">
              <NavContent pathname={pathname} currentType={currentType} onNavClick={closeDrawer} />
            </nav>
          </div>
        </>
      )}
    </>
  );
}

function NavContent({ pathname, currentType, onNavClick, collapsed = false }: {
  pathname: string;
  currentType: string | null;
  onNavClick?: () => void;
  collapsed?: boolean;
}) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());
  // 서버 렌더·클라이언트 첫 렌더는 항상 비로그인 상태로 고정 — silent refresh로 복구된
  // accessToken이 이 Suspense 서브트리의 하이드레이션보다 먼저 반영돼 발생하는 하이드레이션 불일치 방지
  const mounted = useHasMounted();

  return (
    <>
      {mounted && isLoggedIn && (
        <>
          {MY_TABS.map(({ label, href, icon }) => (
            <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} collapsed={collapsed} />
          ))}
          <div className="h-px bg-outline-variant my-3" />
        </>
      )}
      {BOARD_TABS.map(({ label, href, type, icon }) => {
        const active = pathname === ROUTES.board.all && currentType === type;
        return <NavItem key={href} href={href} icon={icon} label={label} active={active} onClick={onNavClick} collapsed={collapsed} />;
      })}
      <div className="h-px bg-outline-variant my-3" />
      {EXTRA_TABS.map(({ label, href, icon }) => (
        <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} collapsed={collapsed} />
      ))}
      <div className="h-px bg-outline-variant my-3" />
      {STATS_TABS.map(({ label, href, icon }) => (
        <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} collapsed={collapsed} />
      ))}
      {!collapsed && (
        <>
          <div className="h-px bg-outline-variant my-3" />
          <footer className="flex items-center gap-2 px-3 pb-1">
            <Link href={ROUTES.terms} onClick={onNavClick} className="text-xs text-center leading-tight text-on-surface-variant hover:text-on-surface transition-colors">
              이용<br />약관
            </Link>
            <span className="text-xs text-outline-variant">·</span>
            <Link href={ROUTES.privacy} onClick={onNavClick} className="text-xs text-center leading-tight text-on-surface-variant hover:text-on-surface transition-colors">
              개인정보<br />처리방침
            </Link>
            <span className="text-xs text-outline-variant">·</span>
            <a href={CONTACT_MAILTO} className="text-xs text-on-surface-variant hover:text-on-surface transition-colors">
              문의
            </a>
          </footer>
        </>
      )}
    </>
  );
}

function NavItem({ href, icon: Icon, label, active, onClick, collapsed = false }: {
  href: string;
  icon: IconType;
  label: string;
  active: boolean;
  onClick?: () => void;
  collapsed?: boolean;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      title={collapsed ? label : undefined}
      className={`flex items-center rounded-xl text-sm font-medium transition-colors ${collapsed ? "justify-center px-2 py-2.5" : "gap-3 px-3 py-2.5"} ${
        active
          ? "bg-sidebar-primary/85 font-semibold text-sidebar"
          : "text-on-surface-variant hover:bg-accent hover:text-foreground"
      }`}
    >
      <Icon size={20} />
      {!collapsed && <span>{label}</span>}
    </Link>
  );
}
