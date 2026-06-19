interface DebateProgressBarProps {
  proPct: number;
  conPct: number;
  height?: string;
}

export default function DebateProgressBar({ proPct, conPct, height = "h-4" }: DebateProgressBarProps) {
  return (
    <div className={`w-full ${height} bg-surface-container rounded-full relative overflow-hidden`}>
      <div
        className="absolute top-0 left-0 h-full w-full"
        style={{ background: "#9a7060" }}
      />
      <div
        className="absolute top-0 left-0 h-full transition-all duration-1000 ease-out"
        style={{
          width: `${proPct}%`,
          background: "linear-gradient(90deg, #7a5645 60%, rgba(122, 86, 69, 0) 100%)",
        }}
      />
      <div
        className="absolute top-0 h-full transition-all duration-1000 ease-out"
        style={{
          left: `${proPct}%`,
          width: `${conPct}%`,
          background: "linear-gradient(90deg, rgba(49, 23, 10, 0) 0%, #31170a 40%)",
        }}
      />
    </div>
  );
}
