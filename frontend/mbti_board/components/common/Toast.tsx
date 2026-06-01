"use client";

import { useCallback, useEffect, useState } from "react";
import { CheckCircle2, XCircle, Info, AlertTriangle, X } from "lucide-react";

type ToastType = "success" | "error" | "info" | "warning";

interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
  duration: number;
}

// 컴포넌트 외부(훅, 이벤트 핸들러 등)에서 toast.success() 형태로 호출하기 위한 모듈 레벨 ref
const _ref: { add: ((type: ToastType, message: string, duration?: number) => void) | null } = {
  add: null,
};

export const toast = {
  success: (message: string, duration?: number) => _ref.add?.("success", message, duration),
  error:   (message: string, duration?: number) => _ref.add?.("error",   message, duration),
  info:    (message: string, duration?: number) => _ref.add?.("info",    message, duration),
  warning: (message: string, duration?: number) => _ref.add?.("warning", message, duration),
};

const TYPE_CONFIG: Record<ToastType, { className: string; icon: React.ReactNode }> = {
  success: {
    className: "bg-green-50 border-green-300 text-green-800",
    icon: <CheckCircle2 size={16} />,
  },
  error: {
    className: "bg-red-50 border-red-300 text-red-800",
    icon: <XCircle size={16} />,
  },
  info: {
    className: "bg-blue-50 border-blue-300 text-blue-800",
    icon: <Info size={16} />,
  },
  warning: {
    className: "bg-orange-50 border-orange-300 text-orange-800",
    icon: <AlertTriangle size={16} />,
  },
};

function ToastCard({ item, onRemove }: { item: ToastItem; onRemove: (id: string) => void }) {
  const [visible, setVisible] = useState(false);
  const { className, icon } = TYPE_CONFIG[item.type];

  useEffect(() => {
    const showFrame  = requestAnimationFrame(() => setVisible(true));
    const hideTimer  = setTimeout(() => setVisible(false), item.duration - 300);
    const removeTimer = setTimeout(() => onRemove(item.id), item.duration);
    return () => {
      cancelAnimationFrame(showFrame);
      clearTimeout(hideTimer);
      clearTimeout(removeTimer);
    };
  }, [item.id, item.duration, onRemove]);

  const handleClose = () => {
    setVisible(false);
    setTimeout(() => onRemove(item.id), 300);
  };

  return (
    <div
      className={`
        flex items-start gap-3 w-80 px-4 py-3 rounded-lg border shadow-md text-sm
        transition-all duration-300 ease-out
        ${visible ? "opacity-100 translate-y-0" : "opacity-0 translate-y-3"}
        ${className}
      `}
    >
      <span className="mt-0.5 shrink-0">{icon}</span>
      <p className="flex-1 leading-snug break-words">{item.message}</p>
      <button
        type="button"
        onClick={handleClose}
        className="shrink-0 opacity-50 hover:opacity-100 transition-opacity"
      >
        <X size={14} />
      </button>
    </div>
  );
}

let _idCounter = 0;

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const add = useCallback((type: ToastType, message: string, duration = 3000) => {
    setToasts((prev) => [...prev, { id: String(++_idCounter), type, message, duration }]);
  }, []);

  const remove = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  useEffect(() => {
    _ref.add = add;
    return () => { _ref.add = null; };
  }, [add]);

  return (
    <>
      {children}
      <div
        role="region"
        aria-label="알림"
        aria-live="polite"
        className="fixed bottom-4 left-4 z-50 flex flex-col gap-2"
      >
        {toasts.map((t) => (
          <ToastCard key={t.id} item={t} onRemove={remove} />
        ))}
      </div>
    </>
  );
}
