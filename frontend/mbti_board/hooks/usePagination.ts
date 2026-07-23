import { useMemo } from "react";

export const PAGE_ELLIPSIS = -1;

function buildPageNumbers(current: number, total: number): number[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages: number[] = [0];
  if (current > 3) pages.push(PAGE_ELLIPSIS);
  const start = Math.max(1, current - 1);
  const end = Math.min(total - 2, current + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (current < total - 4) pages.push(PAGE_ELLIPSIS);
  pages.push(total - 1);
  return pages;
}

type UsePaginationParams = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
};

export function usePagination({ page, totalPages, onPageChange }: UsePaginationParams) {
  const pageNumbers = useMemo(() => buildPageNumbers(page, totalPages), [page, totalPages]);

  return {
    pageNumbers,
    goToPrev: () => onPageChange(Math.max(0, page - 1)),
    goToNext: () => onPageChange(Math.min(totalPages - 1, page + 1)),
  };
}
