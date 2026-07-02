interface Props {
  message?: string;
  className?: string;
}

/** Suspense fallback 등에서 사용하는 로딩 텍스트 — "..." 점 3개가 순차적으로 애니메이션됨 */
export default function LoadingIndicator({
  message = "불러오는 중",
  className = "py-8 text-center text-gray-500",
}: Props) {
  return (
    <p className={className}>
      {message}
      <span className="inline-flex">
        <span className="animate-bounce [animation-delay:-0.3s]">.</span>
        <span className="animate-bounce [animation-delay:-0.15s]">.</span>
        <span className="animate-bounce">.</span>
      </span>
    </p>
  );
}
