import { Suspense } from "react";
import Header from "@/components/common/Header";
import AdminNav from "@/components/admin/AdminNav";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="font-jakarta min-h-screen bg-background">
      <Header />
      <div className="flex mx-auto w-full max-w-300">
        <Suspense>
          <AdminNav />
        </Suspense>
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}
