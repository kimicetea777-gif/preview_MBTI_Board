"use client";

import * as Select from "@radix-ui/react-select";
import { HiCheck, HiChevronDown, HiChevronUp } from "react-icons/hi";
import { MBTI_GROUPS } from "@/types/mbti";
import { cn } from "@/lib/utils";

interface MbtiItem {
  id: string;
}

interface Props {
  value: string;
  onChange: (value: string) => void;
  mbtiList: MbtiItem[];
  disabled?: boolean;
  className?: string;
  placeholder?: string;
}

const ALL = "__ALL__";

export default function MbtiSelect({ value, onChange, mbtiList, disabled, className, placeholder }: Props) {
  const mbtiIds = new Set(mbtiList.map((m) => m.id));
  const hasUnknown = mbtiIds.has("UNKNOWN");

  const radixValue = placeholder !== undefined && value === "" ? ALL : value;
  const handleChange = (v: string) => onChange(v === ALL ? "" : v);

  return (
    <Select.Root value={radixValue} onValueChange={handleChange} disabled={disabled}>
      <Select.Trigger
        className={cn(
          "flex w-full items-center justify-between rounded-lg border border-gray-300 px-3 py-2 text-xs",
          "focus:border-gray-500 focus:outline-none disabled:opacity-50 bg-white",
          "data-placeholder:text-gray-400 text-left",
          className,
        )}
      >
        <Select.Value>
          {radixValue === ALL ? placeholder : radixValue === "UNKNOWN" ? "미설정" : radixValue}
        </Select.Value>
        <Select.Icon asChild>
          <HiChevronDown className="h-4 w-4 text-gray-400 shrink-0" />
        </Select.Icon>
      </Select.Trigger>

      <Select.Portal>
        <Select.Content
          className="z-50 min-w-(--radix-select-trigger-width) max-h-52 overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg"
          position="popper"
          sideOffset={4}
        >
          <Select.ScrollUpButton className="flex items-center justify-center py-1 text-gray-400">
            <HiChevronUp className="h-4 w-4" />
          </Select.ScrollUpButton>

          <Select.Viewport className="p-1">
            {placeholder !== undefined && (
              <>
                <MbtiOption value={ALL} label={placeholder} />
                <Select.Separator className="my-1 h-px bg-gray-100" />
              </>
            )}
            {hasUnknown && (
              <>
                <MbtiOption value="UNKNOWN" label="미설정" />
                <Select.Separator className="my-1 h-px bg-gray-100" />
              </>
            )}

            {MBTI_GROUPS.map((group) => {
              const items = group.ids.filter((id) => mbtiIds.has(id));
              if (items.length === 0) return null;
              return (
                <Select.Group key={group.label} className="grid grid-cols-2">
                  <Select.Label className="col-span-2 px-2 py-1 text-xs font-medium text-gray-400">
                    {group.label}
                  </Select.Label>
                  {items.map((id) => (
                    <MbtiOption key={id} value={id} label={id} />
                  ))}
                </Select.Group>
              );
            })}
          </Select.Viewport>

          <Select.ScrollDownButton className="flex items-center justify-center py-1 text-gray-400">
            <HiChevronDown className="h-4 w-4" />
          </Select.ScrollDownButton>
        </Select.Content>
      </Select.Portal>
    </Select.Root>
  );
}

function MbtiOption({ value, label }: { value: string; label: string }) {
  return (
    <Select.Item
      value={value}
      className="relative flex cursor-pointer select-none items-center rounded px-2 py-1.5 pl-7 text-sm text-gray-700 outline-none data-highlighted:bg-gray-100 data-[state=checked]:font-semibold"
    >
      <Select.ItemIndicator className="absolute left-1.5">
        <HiCheck className="h-4 w-4 text-gray-900" />
      </Select.ItemIndicator>
      <Select.ItemText>{label}</Select.ItemText>
    </Select.Item>
  );
}
