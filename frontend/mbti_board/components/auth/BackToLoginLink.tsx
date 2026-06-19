import Link from "next/link";
import { MdArrowBack } from "react-icons/md";
import { ROUTES } from "@/constants/routes";

export default function BackToLoginLink() {
  return (
    <Link
      href={ROUTES.login}
      className="text-sm font-semibold text-primary hover:text-secondary transition-colors flex items-center gap-1"
    >
      <MdArrowBack className="text-lg" />
      로그인으로 돌아가기
    </Link>
  );
}
