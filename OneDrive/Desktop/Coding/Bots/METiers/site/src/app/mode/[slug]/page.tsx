import type { Metadata } from "next";
import { Suspense } from "react";
import { notFound } from "next/navigation";
import { MODES, MODE_NAMES } from "@/types";
import { getLeaderboard } from "@/lib/data";
import { TierColumnsView } from "@/components/TierColumnsView";
import { LeaderboardFilters } from "./LeaderboardFilters";

export const dynamic = "force-dynamic";
export const revalidate = 0;

type Props = { params: Promise<{ slug: string }>; searchParams: Promise<{ tier?: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  if (!MODES.includes(slug as any)) return { title: "Mode not found" };
  const modeName = MODE_NAMES[slug as keyof typeof MODE_NAMES];
  return {
    title: `${modeName} Rankings`,
    description: `${modeName} tier leaderboard. View rankings and filter by tier.`,
    openGraph: { title: `${modeName} | METiers` },
    twitter: { card: "summary", title: `${modeName} | METiers` },
  };
}

export default async function ModePage({ params, searchParams }: Props) {
  const { slug } = await params;
  const { tier: tierFilter } = await searchParams;

  if (!MODES.includes(slug as any)) notFound();

  const mode = slug as typeof MODES[number];
  const rows = await getLeaderboard(mode, tierFilter as any);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text)]">{MODE_NAMES[mode]} Rankings</h1>
          <p className="mt-1 text-sm text-[var(--text-muted)]">
            Filter by tier. Players grouped by tier columns.
          </p>
        </div>
        <Suspense fallback={<div className="h-12 w-64 animate-pulse rounded-xl bg-[var(--card)]" />}>
          <LeaderboardFilters mode={mode} currentTier={tierFilter} />
        </Suspense>
      </div>
      <TierColumnsView rows={rows} />
    </div>
  );
}
