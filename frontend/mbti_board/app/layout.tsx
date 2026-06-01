import type { Metadata } from "next";
import "./globals.css";
import QueryProvider from "@/components/common/QueryProvider";
import AuthSyncProvider from "@/components/common/AuthSyncProvider";
import { Geist, Plus_Jakarta_Sans } from "next/font/google";
import { cn } from "@/lib/utils";
import { ToastProvider } from "@/components/common/Toast";
import { ThemeProvider } from "next-themes";

const geist = Geist({subsets:['latin'],variable:'--font-sans'});
const plusJakarta = Plus_Jakarta_Sans({subsets:['latin'],variable:'--font-jakarta',weight:['400','500','600','700','800']});

export const metadata: Metadata = {
  title: "MBTI Board",
  description: "MBTI 기반 커뮤니티 플랫폼",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={cn("h-full antialiased", "font-sans", geist.variable, plusJakarta.variable)} suppressHydrationWarning>
      <body className="min-h-full flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false} disableTransitionOnChange themes={["light", "dark", "theme-navy", "theme-rose"]}>
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
