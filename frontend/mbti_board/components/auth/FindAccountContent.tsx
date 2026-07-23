"use client";

import { useState } from "react";
import BackToLoginLink from "@/components/auth/BackToLoginLink";
import { FIND_ACCOUNT_TAB } from "@/types/auth";
import type { FindAccountTab } from "@/types/auth";
import { MdPersonSearch, MdLockReset, MdMarkEmailRead } from "react-icons/md";
import AuthCardHeader from "@/components/auth/AuthCardHeader";
import FindEmailSection from "@/components/auth/FindEmailSection";
import FindPasswordSection from "@/components/auth/FindPasswordSection";

type Tab = FindAccountTab;

const HEADER_CONFIG = {
  [FIND_ACCOUNT_TAB.EMAIL]: {
    icon: <MdPersonSearch className="text-primary text-3xl" />,
    title: "이메일 찾기",
    description: "마이페이지에서 등록한 복구 이메일을 입력해주세요. \n이메일을 찾을 수 있어요.",
  },
  [FIND_ACCOUNT_TAB.PASSWORD]: {
    icon: <MdLockReset className="text-primary text-3xl" />,
    title: "비밀번호 찾기",
    description: "가입 시 가입한 이메일을 입력해주세요. \n이메일로 재설정 링크를 보내드려요.",
  },
  [FIND_ACCOUNT_TAB.PASSWORD_SENT]: {
    icon: <MdMarkEmailRead className="text-primary text-3xl" />,
    title: "이메일이 발송되었습니다.",
    description: "메일함을 확인해 주세요.",
  },
};

interface Props {
  initialTab?: Tab;
}

export default function FindAccountContent({ initialTab = FIND_ACCOUNT_TAB.EMAIL }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>(initialTab);
  const [passwordSent, setPasswordSent] = useState(false);

  const headerKey =
    activeTab === FIND_ACCOUNT_TAB.EMAIL
      ? FIND_ACCOUNT_TAB.EMAIL
      : passwordSent
      ? FIND_ACCOUNT_TAB.PASSWORD_SENT
      : FIND_ACCOUNT_TAB.PASSWORD;

  const { icon, title, description } = HEADER_CONFIG[headerKey];

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setPasswordSent(false);
  };

  return (
    <div className="relative w-full max-w-120 isolate">
      <article
        className="w-full bg-card border border-outline-variant rounded-[1.5rem] overflow-hidden"
        style={{ boxShadow: "0 4px 20px rgba(49, 23, 10, 0.12)" }}
      >
        <AuthCardHeader
          icon={icon}
          title={title}
          description={<span className="whitespace-pre-line">{description}</span>}
        />
        {/* 입력창 (이메일 찾기 / 비밀번호 찾기) */}
        <div className="p-8 md:p-12">
          {activeTab === FIND_ACCOUNT_TAB.EMAIL && <FindEmailSection />}
          {activeTab === FIND_ACCOUNT_TAB.PASSWORD && (
            <FindPasswordSection onPasswordSent={() => setPasswordSent(true)} />
          )}
          <footer className="mt-8 pt-8 border-t border-outline-variant flex flex-col items-center gap-4">
            <BackToLoginLink />
            {!passwordSent && (
              <button
                type="button"
                onClick={() =>
                  handleTabChange(
                    activeTab === FIND_ACCOUNT_TAB.EMAIL
                      ? FIND_ACCOUNT_TAB.PASSWORD
                      : FIND_ACCOUNT_TAB.EMAIL
                  )
                }
                className="text-sm text-muted-foreground underline underline-offset-4 hover:text-on-surface-variant transition-colors"
              >
                {activeTab === FIND_ACCOUNT_TAB.EMAIL ? "· 비밀번호 찾기 ·" : "· 이메일 찾기 ·"}
              </button>
            )}
          </footer>
        </div>
      </article>
    </div>
  );
}
