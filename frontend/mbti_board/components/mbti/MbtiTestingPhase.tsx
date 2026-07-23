"use client";

import { useState } from "react";
import { LuSprout, LuArrowLeft, LuArrowRight } from "react-icons/lu";
import type { TotalQuestion, MbtiTestScores } from "@/types/mbti";
import { computeScores } from "@/lib/mbtiUtils";
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
  onComplete: (scores: MbtiTestScores, funAnswers: Record<number, number>) => void;
}

export function MbtiTestingPhase({ questions, onComplete }: Props) {
  const totalQuestions = questions.length;

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<number, number>>({});        // MBTI: id → score
  const [funAnswers, setFunAnswers] = useState<Record<number, number>>({});  // FUN: id → optionId

  const currentQuestion = questions[currentIndex];
  const isFun = currentQuestion.type === "FUN";
  const currentAnswered = isFun
    ? funAnswers[currentQuestion.id] !== undefined
    : answers[currentQuestion.id] !== undefined;

  function handleAnswer(value: number) {
    let newAnswers = answers;
    let newFunAnswers = funAnswers;

    if (currentQuestion.type === "MBTI") {
      newAnswers = { ...answers, [currentQuestion.id]: value };
      setAnswers(newAnswers);
    } else {
      newFunAnswers = { ...funAnswers, [currentQuestion.id]: value };
      setFunAnswers(newFunAnswers);
    }

    if (currentIndex < totalQuestions - 1) {
      setCurrentIndex((i) => i + 1);
    } else {
      onComplete(computeScores(questions, newAnswers), newFunAnswers);
    }
  }

  return (
    <section className="mb-8">
      <header className="mb-3">
        {/* Progress Bar */}
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

      {/* Question Title */}
      <div className="relative flex items-center justify-center px-1 py-6 w-full h-28 bg-card border-2 border-outline-variant storybook-border shadow-sm text-center">
        <h2 className="font-heading text-lg md:text-lg text-on-surface leading-tight text-balance">
          {currentQuestion.title}
        </h2>
      </div>

      <div className="mt-0 h-8 flex justify-between items-center">
        {currentIndex > 0 ? (
          <button
            onClick={() => setCurrentIndex((i) => i - 1)}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-all hover:-translate-x-1"
          >
            <LuArrowLeft /> 이전 문항
          </button>
        ) : <span />}
        {currentAnswered && (
          <button
            onClick={() => setCurrentIndex((i) => i + 1)}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-all hover:translate-x-1"
          >
            다음 문항 <LuArrowRight />
          </button>
        )}
      </div>

      {/* 선택지 */}
      <div className="flex flex-col gap-1.5">
          {isFun
            ? currentQuestion.options.map((opt) => (
                <OptionButton
                  key={opt.id}
                  isSelected={funAnswers[currentQuestion.id] === opt.id}
                  onClick={() => handleAnswer(opt.id)}
                >
                  {opt.optionText}
                </OptionButton>
              ))
            : ANSWER_OPTIONS.map((opt) => (
                <OptionButton
                  key={opt.label}
                  isSelected={answers[currentQuestion.id] === opt.score}
                  onClick={() => handleAnswer(opt.score)}
                >
                  {opt.label}
                  <span className="ml-1.5 text-xs font-normal text-muted-foreground">({opt.subLabel})</span>
                </OptionButton>
              ))
          }
        </div>

        {/* Garden IMAGE */}
        <div className="-mt-8 pointer-events-none hidden sm:block">
          <GardenProgress currentIndex={currentIndex} totalQuestions={totalQuestions} />
        </div>
    </section>
  );
}
