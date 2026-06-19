"use client";

import { HiX } from "react-icons/hi";

interface ClearableInputProps {
  id?: string;
  type?: "text" | "password" | "email";
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onClear: () => void;
  placeholder?: string;
  minLength?: number;
  maxLength?: number;
  required?: boolean;
  autoFocus?: boolean;
  disabled?: boolean;
  inputClassName?: string;
}

export default function ClearableInput({
  id,
  type = "text",
  value,
  onChange,
  onClear,
  placeholder,
  minLength,
  maxLength,
  required,
  autoFocus,
  disabled,
  inputClassName,
}: ClearableInputProps) {
  return (
    <div className="relative">
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        minLength={minLength}
        maxLength={maxLength}
        required={required}
        autoFocus={autoFocus}
        disabled={disabled}
        className={`${value ? "pr-8" : ""} ${inputClassName ?? ""}`}
      />
      {value && (
        <button
          type="button"
          onClick={onClear}
          tabIndex={-1}
          aria-label="지우기"
          className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 rounded text-outline hover:text-on-surface transition-colors"
        >
          <HiX size={14} />
        </button>
      )}
    </div>
  );
}
