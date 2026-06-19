"use client";

import Link from "next/link";
import { LuLeaf, LuSprout, LuTreePine, LuTreeDeciduous } from "react-icons/lu";
import type { IconType } from "react-icons";
import MbtiTypeCard from "@/components/mbti/MbtiTypeCard";
import IconDivider from "@/components/common/IconDivider";
import { MBTI_GROUPS } from "@/types/mbti";
import { getMbtiById } from "@/constants/mbtiData";
import { ROUTES } from "@/constants/routes";



const DIVIDER_ICONS: IconType[] = [LuTreeDeciduous, LuSprout, LuTreePine];

export default function MbtiListContent() {

  return (
    <div className="mx-auto max-w-5xl px-4 py-12">
      {/* ----------- Header + MBTI Link ----------- */}
      <header className="mb-12 text-center">
        <h1 className="font-heading mb-4 text-4xl font-bold tracking-tight text-ring md:text-5xl">
          MBTI 성격 유형
        </h1>
        <p className="mx-auto mb-8 max-w-xl text-base italic text-muted-foreground">
          16가지 성격 유형을 살펴보세요. 각자의 뿌리와 가지처럼, 저마다 고유한 개성이 있습니다.
        </p>
        <Link
          href={ROUTES.mbti.test}
          className="inline-flex items-center gap-2 rounded-lg border border-ring px-5 py-2.5 text-sm font-semibold text-ring transition-colors hover:bg-ring/10"
        >
          <LuSprout className="h-4 w-4" />
          MBTI 검사 하기
        </Link>

        {/* 구분선 */}
        <IconDivider icon={LuLeaf} iconSize="sm"/>
        
      </header>

      {/* ----------- HEADER + (LINK)MBTI TEST ----------- */}
      <div>
        {MBTI_GROUPS.map(({ label, description, ids }, index) => {
          const isLast = (index === MBTI_GROUPS.length - 1);
          const DividerIcon = DIVIDER_ICONS[index] ?? LuLeaf;
          
          return (
            <section key={label} aria-labelledby={`group-${label}`}>
              {/* header */}
              <div className="mb-10 text-center">
                <h2
                  id={`group-${label}`}
                  className="font-heading mb-2 text-2xl font-semibold text-ring md:text-3xl"
                >
                  {label}
                </h2>
                
                {description && (
                  <p className="text-sm text-muted-foreground">{description}</p>
                )}
              </div>
              
              {/* card */}
              <div className="grid grid-cols-2 gap-5 md:grid-cols-4">
                {ids.map((id) => {
                  const mbti = getMbtiById(id);
                  if (!mbti) return null;
                  return (
                    <Link key={id} href={ROUTES.mbti.detail(id)}>
                      <MbtiTypeCard mbti={mbti}/>
                    </Link>
                  );
                })}
              </div>
              {/* 마지막 구분줄 제외 */}
              {!isLast && <IconDivider icon={DividerIcon} />}
            </section>
          );
        })}
      </div>
    </div>
  );
}
