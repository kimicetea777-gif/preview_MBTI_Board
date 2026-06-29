"use client";

import { LuSprout, LuArrowLeft, LuArrowRight } from "react-icons/lu";
import type { TotalQuestion } from "@/types/mbti";
import { GardenProgress } from "./GardenProgress";
import { OptionButton } from "./OptionButton";

export const ANSWER_OPTIONS = [
  { label: "매우 그렇다", subLabel: "Strongly Agree",    score:  2 },
  { label: "그렇다",      subLabel: "Agree",             score:  1 },
  { label: "보통이다",    subLabel: "Neutral",           score:  0 },
  { label: "아니다",      subLabel: "Disagree",          score: -1 },
  { label: "매우 아니다", subLabel: "Strongly Disagree", score: -2 },
] as const;

interface Props {
  questions: TotalQuestion[];
  currentIndex: number;
  totalQuestions: number;
  answers: Record<number, number>;
  funAnswers: Record<number, number>;
  onAnswer: (value: number) => void;
  onNext: () => void;
  onPrev: () => void;
}

export function MbtiTestingPhase({
  questions,
  currentIndex,
  totalQuestions,
  answers,
  funAnswers,
  onAnswer,
  onNext,
  onPrev,
}: Props) {
  const currentQuestion = questions[currentIndex];
  const isFun = currentQuestion.type === "FUN";
  const currentAnswered = isFun
    ? funAnswers[currentQuestion.id] !== undefined
    : answers[currentQuestion.id] !== undefined;

  return (
    <section className="mb-8">
      <header className="mb-3">
        <div className="flex justify-between items-center px-0.5 mb-3">
          <span className="font-medium text-primary/70 uppercase tracking-widest">
            <span className="text-xl font-bold">{String(currentIndex + 1).padStart(2, "0")}</span>
            <span className="text-xs"> / {totalQuestions}</span>
          </span>
          <div className="flex items-center gap-1.5">
            <LuSprout className="text-xm text-primary" />
            <span className="text-xm text-muted-foreground">Your sprout is growing..</span>
          </div>
        </div>
        <div className="h-4 bg-muted rounded-full overflow-hidden border border-border">
          <div
            className="h-full bg-primary-container rounded-full transition-all duration-700 ease-out shadow-[0_0_12px_rgba(0,0,0,0.15)]"
            style={{ width: `${((currentIndex + 1) / totalQuestions) * 100}%` }}
          />
        </div>
      </header>

      {/* Garden IMAGE */}
      <div className="-mt-8 pointer-events-none">
        <GardenProgress currentIndex={currentIndex} totalQuestions={totalQuestions} />
      </div>

      <div className="relative px-8 py-10 w-full bg-card border-2 border-outline-variant storybook-border shadow-sm mt-3 text-center">
        <h2 className="font-heading text-lg md:text-lg text-on-surface leading-tight text-balance">
          {currentQuestion.title}
        </h2>
      </div>

      <div className="mt-3 h-10 flex justify-between items-center">
        {currentIndex > 0 ? (
          <button
            onClick={onPrev}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-all hover:-translate-x-1"
          >
            <LuArrowLeft /> 이전 문항
          </button>
        ) : <span />}
        {currentAnswered && (
          <button
            onClick={onNext}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-all hover:translate-x-1"
          >
            다음 문항 <LuArrowRight />
          </button>
        )}
      </div>
      
      <div className="flex flex-col gap-3">
          {isFun
            ? currentQuestion.options.map((opt) => (
                <OptionButton
                  key={opt.id}
                  isSelected={funAnswers[currentQuestion.id] === opt.id}
                  onClick={() => onAnswer(opt.id)}
                >
                  {opt.optionText}
                </OptionButton>
              ))
            : ANSWER_OPTIONS.map((opt) => (
                <OptionButton
                  key={opt.label}
                  isSelected={answers[currentQuestion.id] === opt.score}
                  onClick={() => onAnswer(opt.score)}
                >
                  {opt.label}
                  <span className="ml-1.5 text-xs font-normal text-muted-foreground">({opt.subLabel})</span>
                </OptionButton>
              ))
          }
        </div> 
    </section>
  );
}
