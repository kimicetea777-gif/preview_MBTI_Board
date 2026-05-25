import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

/*  shadcn/ui의 cn() 전용 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
