import type { Metadata } from "next";
import "./globals.css";

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
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
