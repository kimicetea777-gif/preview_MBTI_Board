import Logo from "@/components/common/Logo";
import Footer from "@/components/common/Footer";

export const dynamic = "force-dynamic";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* --------------------------BACKGROUND-------------------------- */}
      <div className="fixed inset-0 pointer-events-none -z-10 overflow-hidden">
        <div className="absolute top-[-10%] right-[-5%] w-96 h-96 rounded-full opacity-20 blur-3xl bg-secondary-container" />
        <div className="absolute bottom-[-10%] left-[-5%] w-96 h-96 rounded-full opacity-20 blur-3xl bg-[#c5c9a7]" />
      </div>
      {/* --------------------------HEADER-------------------------- */}
      <header className="flex justify-center">
        <Logo size="lg" />
      </header>
      {/* --------------------------CHILDREN-------------------------- */}
      <main className="flex flex-1 flex-col items-center justify-center px-4 py-2">
        {children}
      </main>
      <Footer />
    </div>
  );
}
