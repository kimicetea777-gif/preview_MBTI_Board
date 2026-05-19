import type { Metadata } from "next";
import "./globals.css";
import QueryProvider from "@/components/common/QueryProvider";
import AuthSyncProvider from "@/components/common/AuthSyncProvider";
import Header from "@/components/common/Header";
import Sidebar from "@/components/common/Sidebar";

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
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <QueryProvider>
          <AuthSyncProvider>
            <Header />
            <div className="flex flex-1 mx-auto w-full max-w-7xl">
              <Sidebar />
              <main className="flex-1 p-6">{children}</main>
            </div>
          </AuthSyncProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
