export default function PostCardSkeleton() {
  return (
    <div className="rounded-xl border border-outline-variant bg-card animate-pulse p-6">
      <div className="flex items-center gap-3 mb-4">
        <div className="rounded-full bg-muted w-10 h-10" />
        <div className="space-y-1.5 flex-1">
          <div className="h-3 bg-muted rounded w-24" />
          <div className="h-3 bg-muted rounded w-16" />
        </div>
        <div className="h-5 bg-muted rounded-full w-12" />
      </div>
      <div className="space-y-2">
        <div className="h-4 bg-muted rounded w-3/4" />
        <div className="h-3 bg-muted rounded w-full" />
        <div className="h-3 bg-muted rounded w-2/3" />
      </div>
    </div>
  );
}
