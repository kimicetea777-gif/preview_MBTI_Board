"use client";

interface Props {
  proLabel: string;
  conLabel: string;
  isPending: boolean;
  onProLabelChange: (value: string) => void;
  onConLabelChange: (value: string) => void;
}

export default function DebateLabelsField({
  proLabel,
  conLabel,
  isPending,
  onProLabelChange,
  onConLabelChange,
}: Props) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <div className="space-y-1.5">
        <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1">
          의견1 레이블 (선택)
        </label>
        <input
          type="text"
          value={proLabel}
          onChange={(e) => onProLabelChange(e.target.value)}
          placeholder="찬성"
          maxLength={50}
          disabled={isPending}
          className="w-full bg-background border border-border rounded-lg px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none disabled:opacity-50 transition-all"
        />
      </div>
      <div className="space-y-1.5">
        <label className="block text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1">
          의견2 레이블 (선택)
        </label>
        <input
          type="text"
          value={conLabel}
          onChange={(e) => onConLabelChange(e.target.value)}
          placeholder="반대"
          maxLength={50}
          disabled={isPending}
          className="w-full bg-background border border-border rounded-lg px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none disabled:opacity-50 transition-all"
        />
      </div>
    </div>
  );
}
