"use client";

import { LuSprout, LuArrowLeft, LuArrowRight } from "react-icons/lu";
import type { TotalQuestion } from "@/types/mbti";
import { GardenProgress } from "./GardenProgress";

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

      <div className="bg-card rounded-2xl border border-border shadow-[0_4px_12px_rgba(0,0,0,0.05)] p-8 pb-0">

        <div className="mb-8">
          <p className="text-lg font-medium h-20 text-card-foreground leading-relaxed text-center">
            {currentQuestion.title}
          </p>
        </div>

        <div className="flex flex-col gap-3">
          {isFun
            ? currentQuestion.options.map((opt) => {
                const isSelected = funAnswers[currentQuestion.id] === opt.id;
                return (
                  <button
                    key={opt.id}
                    onClick={() => onAnswer(opt.id)}
                    className="group w-full text-left"
                  >
                    <div className={`flex items-center gap-4 p-5 rounded-lg border-2 transition-all duration-200 hover:-translate-y-0.5 ${
                      isSelected
                        ? "border-primary bg-primary/10"
                        : "border-border bg-card hover:border-primary/50"
                    }`}>
                      <div className={`size-6 shrink-0 rounded-full border-2 flex items-center justify-center transition-colors ${
                        isSelected ? "border-primary" : "border-muted-foreground group-hover:border-primary/70"
                      }`}>
                        <div className={`size-2 rounded-full bg-primary transition-transform duration-150 ${
                          isSelected ? "scale-100" : "scale-0"
                        }`} />
                      </div>
                      <span className="text-sm font-medium text-card-foreground">
                        {opt.optionText}
                      </span>
                    </div>
                  </button>
                );
              })
            : ANSWER_OPTIONS.map((opt) => {
                const isSelected = answers[currentQuestion.id] === opt.score;
                return (
                  <button
                    key={opt.label}
                    onClick={() => onAnswer(opt.score)}
                    className="group w-full text-left"
                  >
                    <div className={`flex items-center gap-4 p-5 rounded-lg border-2 transition-all duration-200 hover:-translate-y-0.5 ${
                      isSelected
                        ? "border-primary bg-primary/10"
                        : "border-border bg-card hover:border-primary/50"
                    }`}>
                      <div className={`size-6 shrink-0 rounded-full border-2 flex items-center justify-center transition-colors ${
                        isSelected ? "border-primary" : "border-muted-foreground group-hover:border-primary/70"
                      }`}>
                        <div className={`size-2 rounded-full bg-primary transition-transform duration-150 ${
                          isSelected ? "scale-100" : "scale-0"
                        }`} />
                      </div>
                      <span className="text-sm font-medium text-card-foreground">
                        {opt.label}
                        <span className="ml-1.5 text-xs font-normal text-muted-foreground">({opt.subLabel})</span>
                      </span>
                    </div>
                  </button>
                );
              })
          }
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

        <div className="-mt-12 pointer-events-none">
          <GardenProgress currentIndex={currentIndex} totalQuestions={totalQuestions} />
        </div>

      </div>
    </section>
  );
}
