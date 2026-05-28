"use client";

import Link from "next/link";
import { useMbtiList } from "@/hooks/useMbti";
import MbtiTypeCard from "@/components/mbti/MbtiTypeCard";
import { MBTI_GROUPS, NONE_MBTI_ID } from "@/types/mbti";
import { ROUTES } from "@/lib/routes";

function CardSkeleton() {
  return (
    <div className="animate-pulse rounded-xl border border-gray-100 bg-white px-4 py-5">
      <div className="mx-auto mb-3 h-20 w-20 rounded-full bg-gray-200" />
      <div className="mx-auto mb-2 h-4 w-12 rounded bg-gray-200" />
      <div className="mx-auto h-3 w-20 rounded bg-gray-200" />
    </div>
  );
}

export default function MbtiListContent() {
  const { data: mbtiList, isLoading, isError } = useMbtiList();

  const mbtiMap = new Map(
    (mbtiList ?? [])
      .filter((m) => m.id !== NONE_MBTI_ID)
      .map((m) => [m.id, m])
  );

  return (
    <section className="mx-auto max-w-4xl px-4 py-6">
      <div className="mb-8 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">MBTI 성격 유형</h1>
          <p className="mt-1 text-sm text-gray-500">16가지 성격 유형을 살펴보세요</p>
        </div>
        <Link
          href={ROUTES.mbti.test}
          className="shrink-0 rounded-md bg-gray-900 px-4 py-2 text-sm text-white hover:bg-gray-700"
        >
          MBTI 검사 하기
        </Link>
      </div>

      {isError && (
        <p className="py-8 text-center text-sm text-red-500">
          데이터를 불러오는 중 오류가 발생했습니다. 다시 시도해 주세요.
        </p>
      )}

      {!isError && <div className="space-y-10">
        {MBTI_GROUPS.map(({ label, ids }) => (
          <section key={label}>
            <h2 className="mb-4 text-base font-semibold text-gray-600">{label}</h2>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              {isLoading
                ? Array.from({ length: 4 }).map((_, i) => <CardSkeleton key={i} />)
                : ids.map((id) => {
                    const mbti = mbtiMap.get(id);
                    if (!mbti) return null;
                    return <MbtiTypeCard key={id} mbti={mbti} />;
                  })}
            </div>
          </section>
        ))}
      </div>}
    </section>
  );
}
