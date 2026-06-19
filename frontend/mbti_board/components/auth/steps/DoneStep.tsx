import Link from "next/link";
import { ROUTES } from "@/constants/routes";

export default function DoneStep() {
  return (
    <section className="max-w-md mx-auto text-center">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">가입 완료!</h1>
      <article className="bg-white rounded-xl border border-gray-200 p-6 space-y-4">
        <p className="text-sm text-gray-700">
          회원가입이 완료되었습니다. 로그인 후 서비스를 이용하세요.
        </p>
        <Link
          href={ROUTES.login}
          className="block w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 transition-colors"
        >
          로그인하기
        </Link>
      </article>
    </section>
  );
}
