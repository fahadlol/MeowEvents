export default function LeaderboardsLoading() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-8 animate-pulse">
      <div className="mb-8">
        <div className="h-8 w-56 rounded bg-[var(--bg-elevated)]" />
        <div className="mt-2 h-4 w-72 rounded bg-[var(--bg-elevated)]" />
      </div>
      <div className="flex flex-col gap-3">
        {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((i) => (
          <div
            key={i}
            className="flex items-center gap-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] px-5 py-4"
          >
            <div className="h-12 w-12 shrink-0 rounded-xl bg-[var(--bg-elevated)]" />
            <div className="h-12 w-12 shrink-0 rounded-lg bg-[var(--bg-elevated)]" />
            <div className="h-4 flex-1 rounded bg-[var(--bg-elevated)]" />
            <div className="h-6 w-16 rounded bg-[var(--bg-elevated)]" />
            <div className="h-6 w-24 rounded bg-[var(--bg-elevated)]" />
          </div>
        ))}
      </div>
    </div>
  );
}
