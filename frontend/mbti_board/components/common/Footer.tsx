import Link from "next/link";
import { ROUTES } from "@/constants/routes";
import { APP_NAME, CONTACT_MAILTO } from "@/constants/site";

export default function Footer() {
  return (
    <footer className="border-outline-variant mt-auto py-6 px-8 text-xs text-on-surface-variant">
      <div className="mx-auto max-w-300 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>© 2026 {APP_NAME}.</p>
        <nav className="flex items-center gap-4">
          <Link href={ROUTES.terms} className="hover:text-on-surface transition-colors">
            이용약관
          </Link>
          <Link href={ROUTES.privacy} className="hover:text-on-surface transition-colors">
            개인정보처리방침
          </Link>
          <a href={CONTACT_MAILTO} className="hover:text-on-surface transition-colors">
            문의
          </a>
        </nav>
      </div>
    </footer>
  );
}
