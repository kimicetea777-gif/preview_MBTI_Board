interface Props {
  label: string;
  children: React.ReactNode;
}

export default function LabeledFilterBox({ label, children }: Props) {
  return (
    <div className="flex items-center rounded-lg border border-gray-300 overflow-hidden">
      <span className="px-3 py-1.5 text-xs text-gray-500 bg-gray-50 border-r border-gray-300 whitespace-nowrap select-none">
        {label}
      </span>
      {children}
    </div>
  );
}
