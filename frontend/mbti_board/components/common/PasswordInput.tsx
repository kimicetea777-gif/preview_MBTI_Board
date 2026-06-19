"use client";

import { useState } from "react";
import { HiEye, HiEyeOff } from "react-icons/hi";
import FloatingLabelInput from "./FloatingLabelInput";

interface Props {
  id: string;
  label: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  icon?: React.ReactNode;
  disabled?: boolean;
  required?: boolean;
  minLength?: number;
  autoComplete?: string;
  hint?: string;
  error?: boolean;
}

export default function PasswordInput({
  id,
  label,
  value,
  onChange,
  icon,
  disabled,
  required,
  minLength,
  autoComplete = "current-password",
  hint,
  error,
}: Props) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <FloatingLabelInput
      id={id}
      label={label}
      type={showPassword ? "text" : "password"}
      value={value}
      onChange={onChange}
      icon={icon}
      disabled={disabled}
      required={required}
      minLength={minLength}
      autoComplete={autoComplete}
      hint={hint}
      error={error}
      rightSlot={
        <button
          type="button"
          onClick={() => setShowPassword((v) => !v)}
          className="text-gray-400 hover:text-gray-600"
        >
          {value && (showPassword ? <HiEyeOff size={16} /> : <HiEye size={16} />)}
        </button>
      }
    />
  );
}
