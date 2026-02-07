export default function ModeLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8 animate-pulse">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="h-8 w-48 rounded bg-[var(--bg-elevated)]" />
          <div className="mt-2 h-4 w-64 rounded bg-[var(--bg-elevated)]" />
        </div>
        <div className="h-12 w-48 rounded-xl bg-[var(--bg-elevated)]" />
      </div>
      <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-6">
        <div className="flex flex-wrap gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex-1 min-w-[200px]">
              <div className="mb-3 h-5 w-20 rounded bg-[var(--bg-elevated)]" />
              <div className="space-y-2">
                {[1, 2, 3, 4].map((j) => (
                  <div key={j} className="h-10 rounded bg-[var(--bg-elevated)]" />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
