import type { Metadata } from "next";
import Link from "next/link";
import Image from "next/image";
import { getOverallLeaderboard } from "@/lib/data";
import { MODES, MODE_NAMES } from "@/types";
import { ModeIcon } from "@/components/Icons";
import type { OverallLeaderboardRow } from "@/lib/data";
import type { ModeSlug } from "@/types";

export const dynamic = "force-dynamic";
export const revalidate = 0;

export const metadata: Metadata = {
  title: "Overall Rankings",
  description: "Top 100 overall rankings. Sum of tier points across all PvP modes.",
  openGraph: { title: "Overall Rankings | METiers", description: "Top 100 overall rankings by tier points." },
  twitter: { card: "summary", title: "Overall Rankings | METiers" },
};

const MC_AVATAR = "https://mc-heads.net/avatar";

// Region badge colors (inspired by example: NA red, EU green, AS purple, etc.)
function getRegionStyle(region: string): string {
  const r = region.toLowerCase();
  if (r.includes("na") || r.includes("north america")) return "bg-rose-500/90 text-white";
  if (r.includes("eu") || r.includes("europe")) return "bg-emerald-500/90 text-white";
  if (r.includes("as") || r.includes("asia")) return "bg-violet-500/90 text-white";
  if (r.includes("middle east") || r.includes("me")) return "bg-amber-500/90 text-black";
  return "bg-sky-500/90 text-white";
}

// Mode pill background colors for tier badges
const MODE_COLORS: Record<string, string> = {
  sword: "bg-red-500/20 text-red-300 border-red-500/40",
  axe: "bg-blue-500/20 text-blue-300 border-blue-500/40",
  netherite_pot: "bg-purple-500/20 text-purple-300 border-purple-500/40",
  diamond_pot: "bg-cyan-500/20 text-cyan-300 border-cyan-500/40",
  vanilla: "bg-amber-500/20 text-amber-300 border-amber-500/40",
  mace: "bg-orange-500/20 text-orange-300 border-orange-500/40",
  uhc: "bg-pink-500/20 text-pink-300 border-pink-500/40",
  smp: "bg-green-500/20 text-green-300 border-green-500/40",
};

function RankBadge({ rank }: { rank: number }) {
  if (rank === 1) {
    return (
      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-amber-400 via-yellow-500 to-amber-600 text-lg font-bold text-black shadow-lg shadow-amber-500/30">
        {rank}
      </div>
    );
  }
  if (rank === 2) {
    return (
      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-slate-300 via-slate-400 to-slate-500 text-lg font-bold text-black shadow-lg shadow-slate-400/30">
        {rank}
      </div>
    );
  }
  if (rank === 3) {
    return (
      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-amber-600 via-amber-700 to-amber-800 text-lg font-bold text-amber-100 shadow-lg shadow-amber-700/30">
        {rank}
      </div>
    );
  }
  return (
    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[var(--bg-elevated)] text-lg font-bold text-[var(--text-muted)]">
      {rank}
    </div>
  );
}

function LeaderboardRow({ row, rank }: { row: OverallLeaderboardRow; rank: number }) {
  const isTop3 = rank <= 3;
  return (
    <Link
      href={`/player/${encodeURIComponent(row.username)}`}
      className={`group flex flex-wrap items-center gap-4 rounded-xl border px-5 py-4 transition-all duration-200 hover:border-[var(--accent)]/40 hover:bg-[var(--card-hover)] hover:shadow-[0_0_20px_rgba(59,130,246,0.08)] ${
        isTop3
          ? "border-amber-500/30 bg-gradient-to-r from-amber-950/20 to-transparent shadow-[0_0_24px_rgba(251,191,36,0.06)]"
          : "border-[var(--border-subtle)] bg-[var(--card)]"
      }`}
    >
      <RankBadge rank={rank} />
      <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-lg border-2 border-[var(--border-subtle)] bg-[var(--bg-elevated)]">
        <Image
          src={`${MC_AVATAR}/${encodeURIComponent(row.username)}/64`}
          alt=""
          width={48}
          height={48}
          unoptimized
          className="object-cover"
        />
      </div>
      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold text-[var(--text)] group-hover:text-[var(--accent)]">
          {row.username}
        </p>
        <p className="text-sm text-[var(--text-muted)]">
          {row.overall_score} points
        </p>
      </div>
      <span
        className={`shrink-0 rounded-md px-2.5 py-1 text-xs font-medium ${getRegionStyle(row.region)}`}
      >
        {row.region}
      </span>
      <div className="flex flex-wrap items-center gap-2">
        {row.tiers.map((t) => {
          const modeSlug = t.mode as ModeSlug;
          const colorClass = MODE_COLORS[modeSlug] ?? "bg-[var(--bg-elevated)] border-[var(--border)]";
          const label = MODE_NAMES[modeSlug] ?? t.mode;
          return (
            <span
              key={`${t.mode}-${t.tier}`}
              className={`inline-flex items-center gap-1.5 rounded-lg border px-2 py-1 text-xs font-medium ${colorClass}`}
              title={`${label}: ${t.tier}`}
            >
              {MODES.includes(modeSlug as ModeSlug) && (
                <ModeIcon mode={modeSlug as ModeSlug} className="h-3.5 w-3.5 shrink-0 opacity-90" />
              )}
              <span className="font-semibold text-amber-200">{t.tier}</span>
            </span>
          );
        })}
      </div>
    </Link>
  );
}

export default async function LeaderboardsPage() {
  const rows = await getOverallLeaderboard();

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-[var(--text)]">
          Overall Rankings
        </h1>
        <p className="mt-2 text-[var(--text-muted)]">
          Sum of tier points across all modes. Top 100.
        </p>
      </div>

      <div className="flex flex-col gap-3">
        {rows.map((row, i) => (
          <LeaderboardRow key={row.id} row={row} rank={i + 1} />
        ))}
      </div>

      {rows.length === 0 && (
        <div className="rounded-xl border border-[var(--border)] bg-[var(--card)] p-12 text-center text-[var(--text-muted)]">
          No players on the overall leaderboard yet.
        </div>
      )}
    </div>
  );
}
