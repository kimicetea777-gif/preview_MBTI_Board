import { LuCheck } from "react-icons/lu";

interface Props {
  isSelected: boolean;
  onClick: () => void;
  children: React.ReactNode;
}

export function OptionButton({ isSelected, onClick, children }: Props) {
  return (
    <button onClick={onClick} className="group w-full text-left">
      <div className={`flex items-center gap-4 p-5 storybook-border border-2 transition-all duration-200 hover:-translate-y-0.5 ${
        isSelected ? "border-primary bg-primary/10" : "border-border bg-card hover:border-primary/50"
      }`}>
        <div className={`size-6 shrink-0 rounded-full border-2 flex items-center justify-center transition-colors ${
          isSelected ? "border-primary" : "border-muted-foreground group-hover:border-primary/70"
        }`}>
          <LuCheck strokeWidth={3} className={`size-3.5 text-primary transition-all duration-150 ${
            isSelected ? "scale-100 opacity-100" : "scale-0 opacity-0"
          }`} />
        </div>
        <span className="text-sm font-medium text-card-foreground">
          {children}
        </span>
      </div>
    </button>
  );
}
