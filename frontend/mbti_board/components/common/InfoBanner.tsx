import { HiCheckCircle, HiExclamationCircle, HiInformationCircle, HiXCircle } from "react-icons/hi";

export const BANNER_VARIANT = {
  INFO:    "info",
  WARNING: "warning",
  ERROR:   "error",
  SUCCESS: "success",
} as const;

type Variant = typeof BANNER_VARIANT[keyof typeof BANNER_VARIANT];

type BannerItem = {
  variant: Variant;
  text: string;
};

type SingleProps = BannerItem & { legend?: string };
type MultiProps = { items: BannerItem[]; legend?: string };

type InfoBannerProps = SingleProps | MultiProps;

type VariantConfig = {
  Icon: React.ComponentType<{ className?: string; size?: number }>;
  iconClass: string;
  bgClass: string;
  borderClass: string;
  textClass: string;
};

const VARIANT_CONFIG: Record<Variant, VariantConfig> = {
  info:    { Icon: HiInformationCircle, iconClass: "text-secondary",   bgClass: "bg-secondary/10",   borderClass: "border-secondary/30",   textClass: "text-secondary" },
  warning: { Icon: HiExclamationCircle, iconClass: "text-amber-500",   bgClass: "bg-amber-500/10",   borderClass: "border-amber-500/30",   textClass: "text-amber-500" },
  error:   { Icon: HiXCircle,          iconClass: "text-red-500",      bgClass: "bg-red-500/10",     borderClass: "border-red-500/30",     textClass: "text-red-500" },
  success: { Icon: HiCheckCircle,      iconClass: "text-green-600",    bgClass: "bg-green-600/10",   borderClass: "border-green-600/30",   textClass: "text-green-600" },
};

function BannerRow({ variant, text }: BannerItem) {
  const { Icon, iconClass, textClass } = VARIANT_CONFIG[variant];
  return (
    <div className="flex items-center gap-1">
      <Icon size={16} className={`shrink-0 ${iconClass}`} />
      <span className={`text-sm ${textClass}`}>{text}</span>
    </div>
  );
}

export default function InfoBanner(props: InfoBannerProps) {
  const items: BannerItem[] = "items" in props ? props.items : [{ variant: props.variant, text: props.text }];
  const { bgClass, borderClass, textClass } = VARIANT_CONFIG[items[0].variant];
  const legend = props.legend;

  if (legend) {
    return (
      <fieldset className={`border rounded-xl px-4 py-2 ${bgClass} ${borderClass}`}>
        <legend className={`px-2 text-sm font-semibold ${textClass}`}>{legend}</legend>
        <div className="flex flex-col gap-1">
          {items.map((item, i) => (
            <BannerRow key={i} {...item} />
          ))}
        </div>
      </fieldset>
    );
  }

  return (
    <div className={`flex flex-col gap-2 rounded-xl p-2 px-3 border ${bgClass} ${borderClass}`}>
      {items.map((item, i) => (
        <BannerRow key={i} {...item} />
      ))}
    </div>
  );
}

/*
사용 예시)
<InfoBanner variant="warning" text="사진의 크기는 5MB보다 클 수 없습니다." />
<InfoBanner variant="info" text="아이디는 나중에 변경할 수 없습니다." />

<InfoBanner items={[
  { variant: "warning", text: "사진의 크기는 5MB보다 클 수 없습니다." },
  { variant: "info", text: "업로드는 JPG, PNG, GIF만 허용됩니다." },
]} />

*/