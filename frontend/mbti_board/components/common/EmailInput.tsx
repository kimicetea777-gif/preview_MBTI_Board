"use client";

import { HiOutlineMail } from "react-icons/hi";
import FloatingLabelInput from "./FloatingLabelInput";

interface Props {
  id: string;
  label?: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onClear?: () => void;
  disabled?: boolean;
  required?: boolean;
  hint?: string;
  autoComplete?: string;
  error?: boolean;
}

export default function EmailInput({
  id,
  label = "이메일",
  value,
  onChange,
  onClear,
  disabled,
  required,
  hint,
  autoComplete = "email",
  error,
}: Props) {
  return (
    <FloatingLabelInput
      id={id}
      label={label}
      type="email"
      value={value}
      onChange={onChange}
      icon={<HiOutlineMail />}
      onClear={onClear}
      disabled={disabled}
      required={required}
      hint={hint}
      autoComplete={autoComplete}
      error={error}
    />
  );
}
