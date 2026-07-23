export const AD_VARIANT = {
  FLUID: "fluid",
  RECTANGLE: "rectangle",
  VERTICAL: "vertical"
} as const;

export type AdVariant = typeof AD_VARIANT[keyof typeof AD_VARIANT];

export const AD_SLOTS = {
  fluid:      { key: "ad-fluid",     slot: "0000000000", variant: AD_VARIANT.FLUID },
  rectangle:  { key: "ad-rectangle", slot: "0000000000", variant: AD_VARIANT.RECTANGLE },
  vertical:   { key: "ad-vertical",  slot: "0000000000", variant: AD_VARIANT.VERTICAL },
} as const;

export const AD_CLIENT_ID = "itsme";

export const AD_EVERY = 6;

// 초기 오픈 기간에는 광고 노출을 끄기 위한 스위치. true로 바꾸면 인피드/사이드바 광고가 다시 노출됨.
export const AD_ENABLED = false;
