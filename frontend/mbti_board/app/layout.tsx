import type { Metadata } from "next";
import "./globals.css";
import QueryProvider from "@/components/common/QueryProvider";
import AuthSyncProvider from "@/components/common/AuthSyncProvider";
import { Geist } from "next/font/google";
import { cn } from "@/lib/utils";
import { Toaster } from "@/components/ui/sonner";
import { ThemeProvider } from "next-themes";

const geist = Geist({subsets:['latin'],variable:'--font-sans'});

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
    <html lang="ko" className={cn("h-full antialiased", "font-sans", geist.variable)} suppressHydrationWarning>
      <body className="min-h-full flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem={false} disableTransitionOnChange themes={["light", "dark", "theme-navy", "theme-rose"]}>
          <QueryProvider>
            <AuthSyncProvider>
              {children}
            </AuthSyncProvider>
            <Toaster position="bottom-left"/>
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
