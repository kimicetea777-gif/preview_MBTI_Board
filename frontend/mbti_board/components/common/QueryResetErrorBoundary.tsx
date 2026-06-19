"use client";

import { type ReactNode } from "react";
import { QueryErrorResetBoundary } from "@tanstack/react-query";
import ApiErrorBoundary from "./ApiErrorBoundary";

export default function QueryResetErrorBoundary({ children }: { children: ReactNode }) {
  return (
    <QueryErrorResetBoundary>
      {({ reset }) => (
        <ApiErrorBoundary onReset={reset}>
          {children}
        </ApiErrorBoundary>
      )}
    </QueryErrorResetBoundary>
  );
}
