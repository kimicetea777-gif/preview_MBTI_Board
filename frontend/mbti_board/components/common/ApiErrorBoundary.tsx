"use client";

import { Component, type ReactNode } from "react";
import { isAxiosError } from "axios";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onReset?: () => void;
}

interface State {
  hasError: boolean;
  is404: boolean;
  message: string;
}

export default class ApiErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, is404: false, message: "" };

  static getDerivedStateFromError(error: unknown): State {
    if (isAxiosError(error)) {
      if (error.response?.status === 404) return { hasError: true, is404: true, message: "" };
      if (error.response?.status === 403) return { hasError: true, is404: false, message: "관리자 권한이 없습니다." };
    }
    const message = error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.";
    return { hasError: true, is404: false, message };
  }

  handleRetry = () => {
    this.props.onReset?.();
    this.setState({ hasError: false, is404: false, message: "" });
  };

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <div className="flex min-h-50 flex-col items-center justify-center gap-4 text-center">
            <p className="text-sm text-gray-500">
              {this.state.is404 ? "데이터를 찾을 수 없습니다." : this.state.message}
            </p>
            <button
              onClick={this.handleRetry}
              className="rounded-md bg-gray-100 px-4 py-2 text-sm hover:bg-gray-200"
            >
              다시 시도
            </button>
          </div>
        )
      );
    }

    return this.props.children;
  }
}
