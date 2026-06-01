"use client";

import { HiX } from "react-icons/hi";

interface Props {
  id: string;
  label: string;
  type?: "text" | "password" | "email";
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  icon?: React.ReactNode;
  onClear?: () => void;
  rightSlot?: React.ReactNode;
  disabled?: boolean;
  required?: boolean;
  autoComplete?: string;
  minLength?: number;
  maxLength?: number;
  hint?: string;
  error?: boolean;
}

export default function FloatingLabelInput({
  id,
  label,
  type = "text",
  value,
  onChange,
  icon,
  onClear,
  rightSlot,
  disabled,
  required,
  autoComplete,
  minLength,
  maxLength,
  hint,
  error,
}: Props) {
  const hasRightSlot = !!onClear || !!rightSlot;

  return (
    <div className={`relative rounded-lg border px-3 ${error ? "border-red-400 focus-within:border-red-500" : "border-gray-300 focus-within:border-gray-500"}`}>
      <input
        type={type}
        id={id}
        value={value}
        onChange={onChange}
        required={required}
        placeholder=" "
        autoComplete={autoComplete}
        disabled={disabled}
        minLength={minLength}
        maxLength={maxLength}
        className={`peer w-full h-12 py-3 text-sm focus:outline-none disabled:opacity-50 bg-transparent${hasRightSlot ? " pr-7" : ""}`}
      />
      <label
        htmlFor={id}
        className="absolute left-3 flex items-center gap-1 text-gray-400 pointer-events-none transition-all duration-200 bg-white px-1
          text-xs top-0 -translate-y-1/2
          peer-placeholder-shown:top-1/2 peer-placeholder-shown:-translate-y-1/2 peer-placeholder-shown:text-sm
          peer-focus:top-0 peer-focus:-translate-y-1/2 peer-focus:text-xs peer-focus:text-gray-500"
      >
        {icon}
        {label}
      </label>

      {hint && (
        <span
          aria-hidden="true"
          className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-400 pointer-events-none select-none opacity-0 [.peer:focus:placeholder-shown~&]:opacity-100 transition-opacity duration-150"
        >
          {hint}
        </span>
      )}

      {onClear && value && (
        <button
          type="button"
          onClick={onClear}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
        >
          <HiX size={14} />
        </button>
      )}

      {rightSlot && (
        <span className="absolute right-3 top-1/2 -translate-y-1/2">
          {rightSlot}
        </span>
      )}
    </div>
  );
}
