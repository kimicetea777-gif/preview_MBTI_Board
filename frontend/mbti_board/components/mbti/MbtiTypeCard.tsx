import Image from "next/image";
import type { MbtiMetadata } from "@/types/mbti";

interface Props {
  mbti: MbtiMetadata;
}

/** https: URL만 허용 — javascript:, data: 등 비정상 스킴 차단 */
function isSafeUrl(url: string): boolean {
  try {
    return new URL(url).protocol === "https:";
  } catch {
    return false;
  }
}

export default function MbtiTypeCard({ mbti }: Props) {
  const safeIconUrl = mbti.iconUrl && isSafeUrl(mbti.iconUrl) ? mbti.iconUrl : null;

  return (
    <article
      className="flex flex-col items-center rounded-xl border border-gray-100 bg-white px-4 py-5 shadow-sm transition-shadow hover:shadow-md"
      style={{ borderTopColor: mbti.colorCode, borderTopWidth: 4 }}
    >
      {safeIconUrl ? (
        <Image
          src={safeIconUrl}
          alt={mbti.id}
          width={80}
          height={80}
          unoptimized
          className="mb-3 h-20 w-20 rounded-full object-cover"
        />
      ) : (
        <div
          className="mb-3 flex h-20 w-20 items-center justify-center rounded-full text-lg font-bold text-white"
          style={{ backgroundColor: mbti.colorCode }}
        >
          {mbti.id}
        </div>
      )}
      <h3 className="mb-1 text-base font-bold text-gray-900">{mbti.id}</h3>
      <p className="line-clamp-2 text-center text-xs leading-relaxed text-gray-500">
        {mbti.summary}
      </p>
    </article>
  );
}
