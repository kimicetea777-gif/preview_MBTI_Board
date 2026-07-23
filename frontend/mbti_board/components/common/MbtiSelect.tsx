"use client";

import * as Select from "@radix-ui/react-select";
import { HiCheck, HiChevronDown, HiChevronUp } from "react-icons/hi";
import { MBTI_GROUPS, MBTI_ID } from "@/types/mbti";
import { cn } from "@/lib/utils";

interface Props {
  value: string | undefined;
  onChange: (value: string | undefined) => void;
  disabled?: boolean;
  className?: string;
  placeholder?: string;
  align?: "start" | "center" | "end";
}

const ALL = "__ALL__";

export default function MbtiSelect({ value, onChange, disabled, className, placeholder, align = "start" }: Props) {
  const radixValue = value ?? ALL;
  const isAll = radixValue === ALL;
  const handleChange = (v: string) => onChange(v === ALL ? undefined : v);

  return (
    <Select.Root value={radixValue} onValueChange={handleChange} disabled={disabled}>
      <Select.Trigger
        className={cn(
          "flex w-full items-center justify-between rounded-lg border border-border px-3 py-2 text-xs",
          "focus:border-ring focus:outline-none disabled:opacity-50 bg-background",
          "text-left",
          className,
        )}
      >
        <Select.Value className={cn("font-bold", isAll && "text-muted-foreground")}>
          {isAll ? placeholder : radixValue === MBTI_ID.NONE ? "미설정" : radixValue}
        </Select.Value>
        <Select.Icon asChild>
          <HiChevronDown className="h-4 w-4 text-muted-foreground shrink-0" />
        </Select.Icon>
      </Select.Trigger>

      <Select.Portal>
        <Select.Content
          className="z-50 min-w-(--radix-select-trigger-width) max-h-52 overflow-hidden rounded-lg border border-border bg-popover shadow-lg"
          position="popper"
          align={align}
          sideOffset={4}
        >
          <Select.ScrollUpButton className="flex items-center justify-center py-1 text-muted-foreground">
            <HiChevronUp className="h-4 w-4" />
          </Select.ScrollUpButton>

          <Select.Viewport className="p-1">
            {placeholder !== undefined && (
              <>
                <MbtiOption value={ALL} label={placeholder} />
                <Select.Separator className="my-1 h-px bg-border" />
              </>
            )}
            {MBTI_GROUPS.map((group) => (
              <Select.Group key={group.label} className="grid grid-cols-2">
                <Select.Label className="col-span-2 px-2 py-1 text-xs font-medium text-muted-foreground">
                  {group.label}
                </Select.Label>
                {group.ids.map((id) => (
                  <MbtiOption key={id} value={id} label={id} />
                ))}
              </Select.Group>
            ))}
          </Select.Viewport>

          <Select.ScrollDownButton className="flex items-center justify-center py-1 text-muted-foreground">
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
      className="relative flex cursor-pointer select-none items-center rounded px-2 py-1.5 pl-7 text-sm text-foreground outline-none data-highlighted:bg-accent data-[state=checked]:font-semibold"
    >
      <Select.ItemIndicator className="absolute left-1.5">
        <HiCheck className="h-4 w-4 text-foreground" />
      </Select.ItemIndicator>
      <Select.ItemText>{label}</Select.ItemText>
    </Select.Item>
  );
}
