"use client";

import { AD_CLIENT_ID, AD_SLOTS, AD_VARIANT, type AdVariant } from "@/constants/adSlots";
import { useEffect, useId } from "react";
import { usePathname } from "next/navigation";

interface AdSenseProps {
  variant?: AdVariant;
}

export default function AdSenseCard({ variant = AD_VARIANT.FLUID }: AdSenseProps) {
  const pathname = usePathname();
  const instanceId = useId();
  const insKey = `${instanceId}-${pathname}`;

  useEffect(() => {
    try {
      // @ts-expect-error window.adsbygoogle is injected globally by an external script.
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch (err) {
      console.error("AdSense error:", err);
    }
  }, [pathname]);
  

  // 2. 사각형 고정/반응형 스타일 (300x250, 336x280 등 단가 높은 광고 유도용)
  if (variant === AD_VARIANT.RECTANGLE) {
    return (
      <div className="w-full bg-surface-container-high rounded-xl overflow-hidden p-4 flex flex-col items-center justify-center min-h-[280px]">
        <ins
          key={insKey}
          className="adsbygoogle"
          style={{ display: "inline-block", width: "100%", maxWidth: "336px", height: "280px" }}
          data-ad-client={AD_CLIENT_ID}
          data-ad-slot={AD_SLOTS.rectangle.slot}
          data-ad-format="rectangle"
        />
      </div>
    );
  }

  // 3. 💡 세로형 고정/반응형 스타일 (300x600 등 사이드바 전용 웰메이드 광고 유도용)
  if(variant === AD_VARIANT.VERTICAL){
    return (
      <div className="w-full bg-surface-container-high rounded-xl overflow-hidden p-2 flex items-center justify-center hidden lg:flex min-h-[600px]">
        <ins
          key={insKey}
          className="adsbygoogle"
          style={{ display: "inline-block", width: "100%", maxWidth: "300px", height: "600px" }}
          data-ad-client={AD_CLIENT_ID}
          data-ad-slot={AD_SLOTS.vertical.slot}
          data-ad-format="vertical"
        />
      </div>
    );
  }

  // 1. FLUID 인피드 스타일 (자연스럽게 늘어나는 형태)
  return (
    <div className="w-full bg-surface-container-high rounded-xl overflow-hidden p-2 flex items-start justify-center min-h-[250px] lg:min-h-[300px]">
      <ins
        key={insKey}
        className="adsbygoogle"
        style={{ display: "block", width: "100%", minHeight: "250px" }}
        data-ad-client={AD_CLIENT_ID}
        data-ad-slot={AD_SLOTS.fluid.slot}
        data-ad-format="fluid"
        data-full-width-responsive="true"
      />
    </div>
  );
}