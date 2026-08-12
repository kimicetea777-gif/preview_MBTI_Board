"use client";

import { ROUTES } from "@/constants/routes";
import { MdCheck } from "react-icons/md";
import AuthServerLoginButton from "@/components/auth/AuthServerLoginButton";
import Link from "next/link";
import Logo from "../common/Logo";

interface Props {
  redirectTo: string;
}

const GUEST_FEATURES = [
  "자유게시판 글쓰기 가능",
  "댓글 및 대댓글 참여 가능",
  "게시글 조회 투표 현황",
];

export default function LoginForm({ redirectTo }: Props) {
  return (
    <div className="relative w-full max-w-150 isolate">
      <div className="flex flex-col lg:flex-row gap-6 items-stretch">

        <div
          className="flex-1 bg-card border border-outline-variant rounded-[1.5rem] p-8 md:p-12 flex flex-col items-center justify-center text-center gap-6"
          style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.12)" }}
        >
          <div className="mb-4">
            <Logo size="md" />
          </div>
          <div className="space-y-2">
            <h2 className="text-xl font-bold text-primary leading-snug">
              로그인 없이도<br />글쓰기/댓글이 가능해요!
            </h2>
            <p className="text-sm text-on-surface-variant leading-relaxed">
              비회원으로도 커뮤니티에 참여할 수 있어요.
            </p>
          </div>

          <ul className="bg-surface-container-low p-4 rounded-xl border border-outline-variant/40 space-y-3 text-sm text-on-surface-variant text-left w-full max-w-[16rem]">
            {GUEST_FEATURES.map((feature) => (
              <li key={feature} className="flex items-center gap-2">
                <MdCheck className="text-primary text-lg shrink-0" />
                {feature}
              </li>
            ))}
          </ul>

          <div className="mt-2 w-full max-w-[16rem] flex flex-col gap-2">
            <Link
              href={ROUTES.board.all}
              className="w-full h-12 border border-primary text-primary text-sm font-semibold rounded-md hover:bg-primary/5 active:scale-[0.98] transition-all flex items-center justify-center gap-2"
            >
              로그인 없이 둘러보기
            </Link>

            <div className="w-full h-12 border border-primary text-primary text-sm font-semibold rounded-md hover:bg-primary/5 active:scale-[0.98] transition-all flex items-center justify-center gap-2">
              <AuthServerLoginButton redirectTo={redirectTo} />
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
