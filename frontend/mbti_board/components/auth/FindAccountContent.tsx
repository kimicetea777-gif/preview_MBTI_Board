"use client";

import { useState } from "react";
import Link from "next/link";
import { useMutation } from "@tanstack/react-query";
import { findEmail, requestPasswordReset } from "@/lib/api/auth";
import { ROUTES } from "@/lib/routes";
import type { FindEmailResponse } from "@/types/auth";
import type { ApiError } from "@/types/api";

type Tab = "email" | "password";
type PasswordStep = 1 | 2;

interface Props {
  initialTab?: Tab;
}

export default function FindAccountContent({ initialTab = "email" }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>(initialTab);

  // 이메일 찾기 상태
  const [nickname, setNickname] = useState("");
  const [emailResult, setEmailResult] = useState<FindEmailResponse | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);

  // 비밀번호 찾기 상태
  const [email, setEmail] = useState("");
  const [passwordStep, setPasswordStep] = useState<PasswordStep>(1);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const findEmailMutation = useMutation({
    mutationFn: findEmail,
    onSuccess: (data) => {
      setEmailResult(data);
      setEmailError(null);
    },
    onError: (err: ApiError) => {
      setEmailResult(null);
      if (err.response?.status === 404) {
        setEmailError("해당 닉네임을 찾을 수 없습니다.");
      } else {
        setEmailError("오류가 발생했습니다. 다시 시도해 주세요.");
      }
    },
  });

  const requestResetMutation = useMutation({
    mutationFn: requestPasswordReset,
    onSuccess: () => {
      setPasswordStep(2);
      setPasswordError(null);
    },
    onError: () => {
      setPasswordError("오류가 발생했습니다. 다시 시도해 주세요.");
    },
  });

  const handleTabChange = (tab: Tab) => {
    setActiveTab(tab);
    setNickname("");
    setEmailResult(null);
    setEmailError(null);
    setEmail("");
    setPasswordStep(1);
    setPasswordError(null);
    findEmailMutation.reset();
    requestResetMutation.reset();
  };

  return (
    <section className="max-w-md mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">회원 정보 찾기</h1>

      <article className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <nav className="flex border-b border-gray-200">
          <button
            type="button"
            onClick={() => handleTabChange("email")}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${
              activeTab === "email"
                ? "text-gray-900 border-b-2 border-gray-900 -mb-px"
                : "text-gray-400 hover:text-gray-600"
            }`}
          >
            이메일 찾기
          </button>
          <button
            type="button"
            onClick={() => handleTabChange("password")}
            className={`flex-1 py-3 text-sm font-medium transition-colors ${
              activeTab === "password"
                ? "text-gray-900 border-b-2 border-gray-900 -mb-px"
                : "text-gray-400 hover:text-gray-600"
            }`}
          >
            비밀번호 찾기
          </button>
        </nav>

        <div className="p-6">
          {activeTab === "email" && (
            <section>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  setEmailResult(null);
                  setEmailError(null);
                  findEmailMutation.mutate({ nickname });
                }}
                className="space-y-4"
              >
                <div>
                  <label htmlFor="find-email-nickname" className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                    닉네임
                  </label>
                  <input
                    id="find-email-nickname"
                    type="text"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                    required
                    placeholder="가입 시 사용한 닉네임"
                    disabled={findEmailMutation.isPending}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
                  />
                </div>
                <button
                  type="submit"
                  disabled={findEmailMutation.isPending}
                  className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                >
                  {findEmailMutation.isPending ? "찾는 중..." : "찾기"}
                </button>
              </form>

              {emailResult && (
                <aside className="mt-4 rounded-lg bg-gray-50 border border-gray-200 p-4 space-y-1">
                  <p className="text-xs text-gray-400">가입된 이메일</p>
                  <p className="text-sm font-medium text-gray-900">{emailResult.maskedEmail}</p>
                  {emailResult.isSocialOnly && (
                    <p className="text-xs text-gray-500 mt-1">
                      소셜 로그인(Google/Kakao)으로 가입된 계정입니다.
                    </p>
                  )}
                </aside>
              )}

              {emailError && (
                <p className="mt-4 text-sm text-red-500 text-center">{emailError}</p>
              )}
            </section>
          )}

          {activeTab === "password" && (
            <section>
              {passwordStep === 1 ? (
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    setPasswordError(null);
                    requestResetMutation.mutate({ email });
                  }}
                  className="space-y-4"
                >
                  <div>
                    <label htmlFor="find-password-email" className="block text-xs text-gray-400 uppercase tracking-wide mb-1">
                      이메일
                    </label>
                    <input
                      id="find-password-email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      autoComplete="email"
                      placeholder="가입 시 사용한 이메일"
                      disabled={requestResetMutation.isPending}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none disabled:opacity-50"
                    />
                  </div>
                  {passwordError && (
                    <p className="text-xs text-red-500">{passwordError}</p>
                  )}
                  <button
                    type="submit"
                    disabled={requestResetMutation.isPending}
                    className="w-full rounded-lg bg-gray-900 py-2 text-sm text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                  >
                    {requestResetMutation.isPending ? "발송 중..." : "재설정 링크 발송"}
                  </button>
                </form>
              ) : (
                <aside className="space-y-4 text-center">
                  <div className="py-4">
                    <p className="text-sm text-gray-700 font-medium">이메일이 발송되었습니다.</p>
                    <p className="text-sm text-gray-500 mt-1">메일함을 확인해 주세요.</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setPasswordStep(1);
                      requestResetMutation.reset();
                    }}
                    className="text-xs text-gray-400 underline hover:text-gray-600 transition-colors"
                  >
                    다시 받기
                  </button>
                </aside>
              )}
            </section>
          )}
        </div>
      </article>

      <p className="mt-4 text-center text-xs text-gray-400">
        <Link href={ROUTES.login} className="hover:text-gray-600 transition-colors">
          로그인으로 돌아가기
        </Link>
      </p>
    </section>
  );
}
