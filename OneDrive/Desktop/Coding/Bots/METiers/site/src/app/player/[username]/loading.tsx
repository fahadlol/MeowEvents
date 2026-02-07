export default function PlayerLoading() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-12 animate-pulse">
      <div className="rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-6">
        <div className="flex flex-col items-center">
          <div className="h-24 w-24 rounded-full bg-[var(--bg-elevated)]" />
          <div className="mt-4 h-7 w-40 rounded bg-[var(--bg-elevated)]" />
          <div className="mt-2 h-4 w-24 rounded bg-[var(--bg-elevated)]" />
          <div className="mt-4 h-10 w-64 rounded-full bg-[var(--bg-elevated)]" />
          <div className="mt-6 flex flex-wrap justify-center gap-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex flex-col items-center gap-1.5">
                <div className="h-10 w-10 rounded-full bg-[var(--bg-elevated)]" />
                <div className="h-5 w-10 rounded-full bg-[var(--bg-elevated)]" />
              </div>
            ))}
          </div>
          <div className="mt-8 h-4 w-32 rounded bg-[var(--bg-elevated)]" />
        </div>
      </div>
    </div>
  );
}
