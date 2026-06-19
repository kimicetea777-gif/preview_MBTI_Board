import type { MbtiMetadata } from "@/types/mbti";
import MbtiAvatar from "@/components/common/MbtiAvatar";

interface Props {
  mbti: MbtiMetadata;
  card?: boolean;
}

export default function MbtiTypeCard({ mbti }: Props) {
  const imageSection = <MbtiAvatar mbti={mbti} size="lg" className="mb-4" />;

  const content = (
    <>
      {imageSection}
      <h3 className="mb-2 text-xl font-bold text-ring">{mbti.id}</h3>
      <p className="line-clamp-3 text-sm leading-relaxed text-muted-foreground">
        {mbti.descript.title}
      </p>
    </>
  );

  return (
    <article className="group flex flex-col items-center rounded-xl p-6 text-center duration-200 hover:-translate-y-1">
      {content}
    </article>
  );
}
