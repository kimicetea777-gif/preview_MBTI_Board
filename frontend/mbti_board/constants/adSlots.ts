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
