import type { Metadata } from "next";
import "./globals.css";
import QueryProvider from "@/components/common/QueryProvider";
import AuthSyncProvider from "@/components/common/AuthSyncProvider";
import { Geist, Plus_Jakarta_Sans } from "next/font/google";
import { cn } from "@/lib/utils";
import { ToastProvider } from "@/components/common/Toast";
import { ThemeProvider } from "next-themes";
import { SITE_URL, APP_NAME, SITE_DESCRIPTION } from "@/constants/site";

const geist = Geist({subsets:['latin'],variable:'--font-sans'});
const plusJakarta = Plus_Jakarta_Sans({subsets:['latin'],variable:'--font-jakarta',weight:['400','500','600','700','800']});

const TITLE = `${APP_NAME} | MBTI 커뮤니티`;

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: { default: TITLE, template: `%s | ${APP_NAME}` },
  description: SITE_DESCRIPTION,
  openGraph: {
    type: "website",
    locale: "ko_KR",
    siteName: APP_NAME,
    title: APP_NAME,
    description: SITE_DESCRIPTION,
    url: "/",
    images: [{ url: "/images/og.png", width: 1376, height: 768, alt: APP_NAME }],
  },
  twitter: {
    card: "summary_large_image",
    title: APP_NAME,
    description: SITE_DESCRIPTION,
    images: [{ url: "/images/og.png", alt: APP_NAME }],
  },
  robots: {
    index: true,
    follow: true,
  },
  verification: {
    google: process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION,
    other: process.env.NEXT_PUBLIC_NAVER_SITE_VERIFICATION
      ? { "naver-site-verification": process.env.NEXT_PUBLIC_NAVER_SITE_VERIFICATION }
      : undefined,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={cn("h-full antialiased theme-earthen", "font-sans", geist.variable, plusJakarta.variable)} suppressHydrationWarning>
      <body className="min-h-full flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="theme-earthen" enableSystem={false} disableTransitionOnChange themes={["theme-earthen", "light", "dark", "theme-navy", "theme-rose"]}>
          <QueryProvider>
            <ToastProvider>
              <AuthSyncProvider>
                {children}
              </AuthSyncProvider>
            </ToastProvider>
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
