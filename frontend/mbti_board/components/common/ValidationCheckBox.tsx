import { MdCheck, MdClose } from "react-icons/md";

export interface CheckItem {
  isValid: boolean;
  label: string;
}

interface Props {
  checks: CheckItem | CheckItem[];
}

export default function ValidationCheckBox({ checks }: Props) {
  const items = Array.isArray(checks) ? checks : [checks];
  return (
    <div className="flex flex-wrap gap-x-3 gap-y-1">
      {items.map(({ isValid, label }) => (
        <span
          key={label}
          className={`flex items-center gap-1 text-sm ${
            isValid ? "text-emerald-500" : "text-orange-400"
          }`}
        >
          {isValid ? <MdCheck/> : <MdClose/>}
          {label}
        </span>
      ))}
    </div>
  );
}
