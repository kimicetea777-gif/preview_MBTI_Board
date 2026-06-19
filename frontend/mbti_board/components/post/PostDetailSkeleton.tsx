export default function PostDetailSkeleton() {
  return (
    <section className="max-w-225 mx-auto px-4 py-12 space-y-6 animate-pulse">
      <div className="h-4 bg-surface-container rounded-full w-20" />
      <div className="h-10 bg-surface-container rounded-full w-3/4" />
      <div className="h-10 bg-surface-container rounded-full w-1/2" />
      <div className="h-px bg-outline-variant mt-2" />
      <div className="space-y-3 pt-4">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-4 bg-surface-container-high rounded-full" />
        ))}
      </div>
      <div className="h-52 bg-surface-container rounded-2xl mt-6" />
    </section>
  );
}
