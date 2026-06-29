import Link from "next/link";
import { ROUTES } from "@/constants/routes";

export default function Footer() {
  return (
    <footer className="border-t border-outline-variant mt-auto py-6 px-8 text-xs text-on-surface-variant">
      <div className="mx-auto max-w-300 flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>© 2025 {process.env.NEXT_PUBLIC_APP_NAME}. All rights reserved.</p>
        <nav className="flex items-center gap-4">
          <Link href={ROUTES.terms} className="hover:text-on-surface transition-colors">
            이용약관
          </Link>
          <Link href={ROUTES.privacy} className="hover:text-on-surface transition-colors">
            개인정보처리방침
          </Link>
        </nav>
      </div>
    </footer>
  );
}
