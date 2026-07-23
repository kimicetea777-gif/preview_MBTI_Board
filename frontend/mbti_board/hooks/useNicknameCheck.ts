"use client";

import { useEffect, useState } from "react";
import { NICKNAME_REGEX } from "@/lib/validationConstants";
import { checkNickname } from "@/lib/api/user";

interface NicknameCheckResult {
  nickname: string;
  available: boolean;
}

interface UseNicknameCheckOptions {
  /** 이 값과 같으면 중복 체크를 건너뜀 (예: 프로필 수정 시 본인의 기존 닉네임) */
  skipValue?: string;
}

/** 닉네임 실시간 중복 체크 — 400ms 디바운스 후 GET /api/users/check-nickname 호출 */
export function useNicknameCheck(nickname: string, options?: UseNicknameCheckOptions) {
  const [result, setResult] = useState<NicknameCheckResult | null>(null);
  const isValidFormat = NICKNAME_REGEX.test(nickname);
  const shouldCheck = isValidFormat && nickname !== options?.skipValue;

  useEffect(() => {
    if (!shouldCheck) return;
    let ignore = false;
    const timer = setTimeout(() => {
      checkNickname(nickname)
        .then(({ available }) => {
          if (!ignore) setResult({ nickname, available });
        })
        .catch(() => {});
    }, 400);
    return () => {
      ignore = true;
      clearTimeout(timer);
    };
  }, [nickname, shouldCheck]);

  const hasResult = shouldCheck && result?.nickname === nickname;
  return {
    available: hasResult && result.available,
    taken: hasResult && !result.available,
  };
}
