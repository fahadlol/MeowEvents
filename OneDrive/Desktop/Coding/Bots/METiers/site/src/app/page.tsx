import Link from "next/link";
import { getLeaderboardPreview } from "@/lib/data";
import { isSupabaseConfigured } from "@/lib/supabase";
import { LeaderboardTable } from "@/components/LeaderboardTable";
import { MODE_NAMES } from "@/types";
import { HomeHero } from "@/components/HomeHero";
import type { LeaderboardRow } from "@/lib/data";

export default async function HomePage() {
  const configured = isSupabaseConfigured();
  let preview: LeaderboardRow[] = [];
  if (configured) {
    try {
      preview = await getLeaderboardPreview("sword", 10);
    } catch {
      preview = [];
    }
  }
  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      {!configured && (
        <div className="mb-6 animate-in-slide-up rounded-xl border border-amber-500/50 bg-amber-500/10 px-4 py-3 text-amber-200 shadow-lg">
          <p className="font-medium">Setup required</p>
          <p className="mt-1 text-sm text-amber-200/90">
            Add <code className="rounded bg-black/20 px-1">NEXT_PUBLIC_SUPABASE_URL</code> and{" "}
            <code className="rounded bg-black/20 px-1">NEXT_PUBLIC_SUPABASE_ANON_KEY</code> to{" "}
            <code className="rounded bg-black/20 px-1">.env.local</code> from the Site section in the root <code className="rounded bg-black/20 px-1">.env.example</code>. Restart the dev server after saving.
          </p>
        </div>
      )}
      <HomeHero />
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-[var(--text)]">
            {MODE_NAMES.sword} — Preview
          </h2>
          <Link
            href="/mode/sword"
            className="text-sm font-medium text-[var(--accent)] transition hover:opacity-80 hover:underline"
          >
            Full leaderboard →
          </Link>
        </div>
        <LeaderboardTable rows={preview} mode="sword" />
      </section>
    </div>
  );
}
