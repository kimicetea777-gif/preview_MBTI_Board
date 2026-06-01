import { Suspense } from "react";
import Header from "@/components/common/Header";
import Sidebar from "@/components/common/Sidebar";

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="font-jakarta min-h-screen bg-background">
      <Header />
      <div className="flex mx-auto w-full max-w-300">
        <Suspense>
          <Sidebar />
        </Suspense>
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}
