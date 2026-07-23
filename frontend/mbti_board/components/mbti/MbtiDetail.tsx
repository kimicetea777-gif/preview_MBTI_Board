"use client";

import Image from "next/image";
import { LuSun, LuWind } from "react-icons/lu";
import type { MbtiMetadata } from "@/types/mbti";
import { ROUTES } from "@/constants/routes";
import { ShareButton } from "@/components/common/ShareButton";
import { getMbtiById } from "@/constants/mbtiData";


const TRAIT_BORDER = [
  { border: "border-primary",   icon: "text-primary"   },
  { border: "border-orange-300", icon: "border-orange-300" },
  { border: "border-rose-500",  icon: "text-rose-500"  },
  { border: "border-slate-500", icon: "text-slate-500" },
] as const;


function PartnerCard({
  meta,
  label,
  LabelIcon,
  labelColorClass,
  cardExtraClass = "",
  iconBgStyle,
  iconBgClass = "",
  iconColorClass = "",
  iconColorStyle,
  description,
}: {
  meta: MbtiMetadata;
  label: string;
  LabelIcon: React.ElementType;
  labelColorClass: string;
  cardExtraClass?: string;
  iconBgStyle?: React.CSSProperties;
  iconBgClass?: string;
  iconColorClass?: string;
  iconColorStyle?: React.CSSProperties;
  description: string;
}) {
  const MetaIcon = meta.icon;
  return (
    <div className="space-y-3">
      <span className={`flex items-center gap-1.5 text-ms font-semibold ${labelColorClass}`}>
        <LabelIcon className="text-ms" />
        {label}
      </span>
      <div className={`flex items-center gap-4 bg-card rounded-xl p-4 border border-border shadow-sm ${cardExtraClass}`}>
        <div
          className={`size-12 shrink-0 rounded-full flex items-center justify-center ${iconBgClass}`}
          style={iconBgStyle}
        >
          <MetaIcon className={`text-xl ${iconColorClass}`} style={iconColorStyle} />
        </div>
        <div>
          <h4 className={`text-sm font-bold ${iconColorClass || "text-foreground"}`}>
            {meta.id} — {meta.title}
          </h4>
          <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
        </div>
      </div>
    </div>
  );
}

interface MbtiDetailProps {
  mbtiId: string;
  /** 공유 버튼 왼쪽에 추가할 액션 버튼 (예: 저장 버튼) */
  actionSlot?: React.ReactNode;
}

export function MbtiDetail({ mbtiId, actionSlot }: MbtiDetailProps) {

  const mbtiMeta = getMbtiById(mbtiId);
  const bestMeta = getMbtiById(mbtiMeta?.bestMbti) ?? null;
  const badMeta = getMbtiById(mbtiMeta?.badMbti)  ?? null;
  const MbtiIcon = mbtiMeta?.icon;

  return (
    <section className="p-6">

      {/* ---- HEADER ---- */}
      <header className="text-center mb-12">
        <span className="inline-block px-4 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold tracking-widest mb-4">
          MBTI 유형
        </span>
        <h1 className="text-4xl font-bold text-primary tracking-wide mb-1">
          {mbtiId}
        </h1>
        {mbtiMeta?.title && (
          <span className="text-xl text-muted-foreground italic font-normal">
            &ldquo;{mbtiMeta.title}&rdquo;
          </span>
        )}
      </header>

      <div className="flex flex-col gap-8">
        <div className="flex flex-col items-center gap-4">

          {/* ---- IMAGE BOX ---- */}
          <div className="relative w-72 sm:w-80 md:w-100 aspect-square rounded-2xl border border-border bg-secondary-container/10 flex items-center justify-center overflow-hidden shadow-sm">
            {mbtiMeta?.imageBackUrl ? (
              <Image
                src={mbtiMeta.imageBackUrl}
                alt={mbtiId}
                fill
                sizes="(max-width: 639px) 288px, (max-width: 767px) 320px, 400px"
                className="object-contain z-10 p-3 rounded-4xl"
              />
            ) : null}
          </div>

          {/* ---- SHARE BUNTTON ---- */}
          <div className="flex flex-col items-center gap-2">
            <div className="flex gap-3">
              {actionSlot}
              <ShareButton path={ROUTES.mbti.detail(mbtiId)} />
            </div>
          </div>
        </div>

        <div className="w-full space-y-8">
          {/* ---- DESCRIPT ---- */}
          {mbtiMeta?.descript && (
            <article className="bg-card rounded-2xl border border-border p-8 shadow-sm">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-primary mb-4">
                {MbtiIcon && <MbtiIcon className="text-xl" />}
                {mbtiMeta.descript.title}
              </h2>
              <p className="text-sm text-foreground leading-loose">
                {mbtiMeta.descript.content}
              </p>
            </article>
          )}

          {mbtiMeta?.personalities && mbtiMeta.personalities.length > 0 && (
            <div>
              {/* 구분선 */}
              <div className="flex items-center gap-4 mb-6">
                <div className="flex-1 h-px bg-border" />
                <span className="text-lg font-semibold text-muted-foreground px-2">
                  성격의 뿌리
                </span>
                <div className="flex-1 h-px bg-border" />
              </div>
              {/* Trait Card */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {mbtiMeta.personalities.map((p, i) => {
                  const TraitIcon = p.icon;
                  return (
                  <div
                    key={i}
                    className={`p-5 bg-card rounded-xl border-l-4 border border-border shadow-sm ${TRAIT_BORDER[i % 4].border}`}
                  >
                    <TraitIcon className={`text-xl mb-2 ${TRAIT_BORDER[i % 4].icon}`} />
                    <h3 className="text-xm font-bold tracking-wide text-foreground mb-1">
                      {p.label}
                    </h3>
                    <p className="text-xm text-muted-foreground leading-relaxed">
                      {p.description}
                    </p>
                  </div>
                  );
                })}
              </div>
            </div>
          )}

          {(bestMeta || badMeta) && (
            <div className="bg-muted rounded-2xl border border-border p-8 shadow-sm">
              <h2 className="text-lg font-semibold text-primary text-center mb-8">
                함께 숲을 거닐 파트너
              </h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                {bestMeta && (
                  <PartnerCard
                    meta={bestMeta}
                    label="맑은 날의 동반자 (Best Fit)"
                    LabelIcon={LuSun}
                    labelColorClass="text-primary"
                    iconBgStyle={{ backgroundColor: `${bestMeta.colorCode}20` }}
                    iconColorStyle={{ color: bestMeta.colorCode }}
                    description="서로의 가치관을 깊이 존중합니다."
                  />
                )}
                {badMeta && (
                  <PartnerCard
                    meta={badMeta}
                    label="안개 속의 조심스러움 (Caution)"
                    LabelIcon={LuWind}
                    labelColorClass="text-muted-foreground"
                    cardExtraClass="opacity-70 grayscale-[0.4]"
                    iconBgClass="bg-muted"
                    iconColorClass="text-muted-foreground"
                    description="서로의 소통 방식 이해가 필요해요."
                  />
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
