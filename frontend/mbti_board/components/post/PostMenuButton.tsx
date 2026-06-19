import { useState, type ReactNode } from "react";
import { MdMoreVert } from "react-icons/md";

export interface PostMenuItem {
  label: string;
  icon?: ReactNode;
  onClick: () => void;
  disabled?: boolean;
  destructive?: boolean;
}

interface Props {
  items: PostMenuItem[];
}

export default function PostMenuButton({ items }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="p-1.5 rounded-lg text-on-surface-variant hover:bg-surface-container transition-colors"
      >
        <MdMoreVert className="text-xl" />
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 top-full mt-1 z-20 min-w-32 overflow-hidden rounded-lg border border-outline-variant bg-surface-container-lowest shadow-md">
            {items.map((item, i) => (
              <button
                key={i}
                type="button"
                onClick={() => { setOpen(false); item.onClick(); }}
                disabled={item.disabled}
                className={`flex w-full items-center gap-2 px-4 py-2.5 text-sm transition-colors disabled:opacity-50 ${
                  item.destructive
                    ? "text-destructive hover:bg-destructive/5"
                    : "text-on-surface hover:bg-surface-container"
                }`}
              >
                {item.icon}
                {item.label}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
