"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import type { IconType } from "react-icons";
import { MdClose } from "react-icons/md";
import { ROUTES } from "@/constants/routes";
import { BOARD_TABS, MY_TABS, EXTRA_TABS, STATS_TABS } from "@/constants/navigation";
import { useAuthStore } from "@/store/authStore";
import AdSenseCard from "./AdSenseCard";
import { AD_SLOTS, AD_VARIANT } from "@/constants/adSlots";
import { useNavStore } from "@/store/navStore";
import { useIsDesktop } from "@/hooks/useIsDesktop";
import Logo from "./Logo";

export default function SiteNav() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentType = searchParams.get("type");
  const { drawerOpen, closeDrawer } = useNavStore();
  
  const isDesktop = useIsDesktop();

  return (
    <>
      {/* 데스크탑 사이드바 */}
      <aside className="hidden md:flex flex-col gap-3 w-57 shrink-0 sticky top-16 self-start py-6 px-4">
        <div className="rounded-2xl border border-outline-variant bg-card px-3 py-4">
          <nav className="flex flex-col">
            <NavContent pathname={pathname} currentType={currentType} />
          </nav>
        </div>
        {isDesktop && <AdSenseCard key={AD_SLOTS.vertical.key} variant={AD_VARIANT.VERTICAL} />}
      </aside>

      {/* 모바일 드로어 */}
      {drawerOpen && (
        <>
          <div className="fixed inset-0 z-40 bg-black/40" onClick={closeDrawer} />
          <div className="fixed inset-y-0 left-0 z-50 w-64 bg-background shadow-xl flex flex-col rounded-r-3xl">
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

function NavContent({ pathname, currentType, onNavClick }: {
  pathname: string;
  currentType: string | null;
  onNavClick?: () => void;
}) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn());

  return (
    <>
      {isLoggedIn && (
        <>
          {MY_TABS.map(({ label, href, icon }) => (
            <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} />
          ))}
          <div className="h-px bg-outline-variant my-3" />
        </>
      )}
      {BOARD_TABS.map(({ label, href, type, icon }) => {
        const active = pathname === ROUTES.board.all && currentType === type;
        return <NavItem key={href} href={href} icon={icon} label={label} active={active} onClick={onNavClick} />;
      })}
      <div className="h-px bg-outline-variant my-3" />
      {EXTRA_TABS.map(({ label, href, icon }) => (
        <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} />
      ))}
      <div className="h-px bg-outline-variant my-3" />
      {STATS_TABS.map(({ label, href, icon }) => (
        <NavItem key={href} href={href} icon={icon} label={label} active={pathname === href} onClick={onNavClick} />
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
