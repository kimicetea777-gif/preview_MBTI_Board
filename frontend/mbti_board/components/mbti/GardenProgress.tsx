"use client";

import Image from "next/image";

interface LayerConfig {
  src: string;
  width: number;
  height: number;
  clips: Record<number, string>;
  className?: string;
}

const layer = (bottom: string) => `absolute ${bottom} -left-16 w-[120%]`;

const LAYERS: LayerConfig[] = [
  {
    src: "/images/mbti/elements/ground.png",
    className: layer("bottom-0"),
    width: 980,
    height: 420,
    clips: {
      0: "inset(0 0 0 0)",
    },
  },
  {
    src: "/images/mbti/elements/vegetable.png",
    className: layer("bottom-17"),
    width: 980,
    height: 440,
    clips: {
      0:  "inset(0 100% 0 0)",
      3:  "inset(0 61.5% 0 0)",
      8:  "inset(0 47% 0 0)",
      16: "inset(0 36% 0 0)",
      24: "inset(0 0 0 0)",
    },
  },  //inset(시작_Y 끝_X 끝_Y 시작_X)    polygon(X Y, X Y, X Y, ...)
  {
    src: "/images/mbti/elements/animals.png",
    className: layer("bottom-10"),
    width: 980,
    height: 420,
    clips: {
      0:  "inset(0 100% 0 0)",
      6:  "inset(0 60% 0 0)",
      18: "inset(0 48% 0 0)",
      26: "inset(0 0 0 0)",
    },
  },
  {
    src: "/images/mbti/elements/fence.png",
    className: layer("-bottom-4"),
    width: 980,
    height: 420,
    clips: {
      0: "inset(0 100% 0 0)", 
      28: "inset(0 0 0 0)",
    },
  },
  
];

function resolveClip(clips: Record<number, string>, index: number): string {
  const keys = Object.keys(clips).map(Number).sort((a, b) => a - b);
  let result = clips[keys[0]];
  for (const key of keys) {
    if (key <= index) result = clips[key];
    else break;
  }
  return result;
}

interface Props {
  currentIndex: number;
  totalQuestions: number;
}

export function GardenProgress({ currentIndex, totalQuestions }: Props) {
  const idx = currentIndex >= totalQuestions - 1 ? totalQuestions : currentIndex;

  return (
    <div className="relative mx-auto w-80 h-26 md:w-140 md:h-45 select-none overflow-hidden mt-0">
      {LAYERS.map((layer) => (
        <div
          key={layer.src}
          className={`pointer-events-none ${layer.className ?? "absolute bottom-0 left-0 w-full"}`}
          style={{ clipPath: resolveClip(layer.clips, idx) }}
        >
          <Image
            src={layer.src}
            alt=""
            aria-hidden="true"
            width={layer.width}
            height={layer.height}
            className="w-full h-auto"
            draggable={false}
            priority
          />
        </div>
      ))}
    </div>
  );
}
