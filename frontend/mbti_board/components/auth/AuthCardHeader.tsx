import type { ReactNode } from "react";

interface Props {
  icon: ReactNode;
  step?: { current: number; total: number };
  title?: string;
  description?: ReactNode;
}

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="mb-6 flex flex-col items-center gap-2">
      <div className="flex gap-2 items-center">
        {Array.from({ length: total }, (_, i) => {
          const s = i + 1;
          return (
            <div
              key={s}
              className={`h-1 rounded-full ${s === current ? "w-12 bg-secondary" : s < current ? "w-8 bg-secondary-container" : "w-8 bg-surface-container"}`}
            />
          );
        })}
      </div>
      <span className="text-xs text-on-surface-variant uppercase tracking-widest">
        Step {current} of {total}
      </span>
    </div>
  );
}

export default function AuthCardHeader({ icon, step, title, description }: Props) {
  return (
    <div className="bg-secondary-container/20 px-8 pt-8 pb-8 text-center border-b border-dashed border-outline-variant/50">
      {step && <StepIndicator current={step.current} total={step.total} />}
      <div className="flex justify-center">
        <div className={`inline-flex items-center justify-center w-16 h-16 rounded-full bg-[#ffdbcb] ${title || description ? "mb-5" : ""}`}>
          {icon}
        </div>
      </div>
      {title && <h1 className="text-2xl font-bold text-primary mb-2">{title}</h1>}
      {description && <p className="text-sm text-on-surface-variant leading-relaxed">{description}</p>}
    </div>
  );
}
