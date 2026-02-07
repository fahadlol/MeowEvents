import { cache } from "react";
import { getSupabase } from "./supabase";
import type { ModeSlug, Tier } from "@/types";
import { tierRank, tierPoints } from "@/types";

function sortByTier<T extends { tier: Tier; last_tested_at: string }>(rows: T[]): T[] {
  return [...rows].sort(
    (a, b) =>
      tierRank(a.tier) - tierRank(b.tier) ||
      new Date(b.last_tested_at).getTime() - new Date(a.last_tested_at).getTime()
  );
}

export interface LeaderboardRow {
  id: string;
  username: string;
  region: string;
  tier: Tier;
  peak_tier?: string | null;
  last_tested_at: string;
  tester_name: string;
}

export interface OverallLeaderboardRow {
  id: string;
  username: string;
  region: string;
  overall_score: number;
  mode_count: number;
  tiers: { mode: string; tier: string; points: number }[];
}

export async function getLeaderboard(
  mode: ModeSlug,
  tierFilter?: Tier
): Promise<LeaderboardRow[]> {
  const supabase = getSupabase();
  if (!supabase) return [];
  let q = supabase
    .from("player_tiers")
    .select(
      `
      id,
      tier,
      peak_tier,
      last_tested_at,
      tester_name,
      player:players(id, username, region)
    `
    )
    .eq("mode", mode)
    .order("tier", { ascending: true })
    .order("last_tested_at", { ascending: false });

  if (tierFilter) {
    q = q.eq("tier", tierFilter);
  }

  const { data, error } = await q;
  if (error) return [];

  const rows = (data || []).map((row: any) => ({
    id: row.id,
    username: row.player?.username ?? "",
    region: row.player?.region ?? "Middle East",
    tier: row.tier,
    peak_tier: row.peak_tier ?? null,
    last_tested_at: row.last_tested_at,
    tester_name: row.tester_name,
  }));
  return sortByTier(rows as LeaderboardRow[]);
}

export async function getLeaderboardByUsername(
  mode: ModeSlug,
  search: string
): Promise<LeaderboardRow[]> {
  const supabase = getSupabase();
  if (!supabase) return [];
  const { data: players } = await supabase
    .from("players")
    .select("id")
    .ilike("username", `%${search}%`);
  const ids = (players || []).map((p) => p.id);
  if (ids.length === 0) return [];

  const { data, error } = await supabase
    .from("player_tiers")
    .select(
      `
      id,
      tier,
      peak_tier,
      last_tested_at,
      tester_name,
      player:players(id, username, region)
    `
    )
    .eq("mode", mode)
    .in("player_id", ids)
    .order("tier", { ascending: true });

  if (error) return [];
  const rows = (data || []).map((row: any) => ({
    id: row.id,
    username: row.player?.username ?? "",
    region: row.player?.region ?? "",
    tier: row.tier,
    peak_tier: row.peak_tier ?? null,
    last_tested_at: row.last_tested_at,
    tester_name: row.tester_name,
  }));
  return sortByTier(rows as LeaderboardRow[]);
}

/** Cached per request so profile metadata + page share one fetch. */
export const getPlayerByUsername = cache(async (username: string) => {
  const supabase = getSupabase();
  if (!supabase) return null;
  const { data: player, error } = await supabase
    .from("players")
    .select("*")
    .ilike("username", username)
    .single();
  if (error || !player) return null;

  const { data: tiers } = await supabase
    .from("player_tiers")
    .select("mode, tier, peak_tier, last_tested_at, tester_name")
    .eq("player_id", player.id);

  const tierByMode: Record<string, { tier: string; peak_tier?: string | null; last_tested_at: string; tester_name: string }> = {};
  (tiers || []).forEach((t) => {
    tierByMode[t.mode] = {
      tier: t.tier,
      peak_tier: t.peak_tier ?? null,
      last_tested_at: t.last_tested_at,
      tester_name: t.tester_name,
    };
  });

  return {
    ...player,
    tiers: tierByMode,
  };
});

export async function getLeaderboardPreview(mode: ModeSlug, limit = 10): Promise<LeaderboardRow[]> {
  try {
    const rows = await getLeaderboard(mode);
    return rows.slice(0, limit);
  } catch {
    return [];
  }
}

const OVERALL_TOP = 100;

/** Overall leaderboard: sum of tier points per player (current tier only). No min modes; returns top 100. */
export async function getOverallLeaderboard(): Promise<OverallLeaderboardRow[]> {
  const full = await getOverallLeaderboardFull(0);
  return full.slice(0, OVERALL_TOP);
}

/** Full overall ranking (no limit). Cached per request so profile only builds once. */
export const getOverallLeaderboardFull = cache(
  async (minModes = 0): Promise<OverallLeaderboardRow[]> => {
    const supabase = getSupabase();
    if (!supabase) return [];
    const { data: allTiers, error } = await supabase
      .from("player_tiers")
      .select("player_id, mode, tier, player:players(id, username, region)");
    if (error || !allTiers) return [];

    const byPlayer = new Map<
      string,
      { username: string; region: string; modes: { mode: string; tier: string; points: number }[] }
    >();
    for (const row of allTiers as any[]) {
      const pid = row.player_id;
      const player = row.player;
      if (!player?.id) continue;
      if (!byPlayer.has(pid)) {
        byPlayer.set(pid, {
          username: player.username ?? "",
          region: player.region ?? "Middle East",
          modes: [],
        });
      }
      const entry = byPlayer.get(pid)!;
      const points = tierPoints(row.tier);
      entry.modes.push({ mode: row.mode, tier: row.tier, points });
    }

    const result: OverallLeaderboardRow[] = [];
    for (const [playerId, entry] of byPlayer.entries()) {
      if (entry.modes.length < minModes) continue;
      const overall_score = entry.modes.reduce((s, m) => s + m.points, 0);
      result.push({
        id: playerId,
        username: entry.username,
        region: entry.region,
        overall_score,
        mode_count: entry.modes.length,
        tiers: entry.modes,
      });
    }
    result.sort((a, b) => b.overall_score - a.overall_score);
    return result;
  }
);

/** Overall rank and total tested players for a player (by id). */
export async function getOverallRankAndTotal(
  playerId: string
): Promise<{ rank: number; total: number } | null> {
  const full = await getOverallLeaderboardFull(0);
  const index = full.findIndex((r) => r.id === playerId);
  if (index === -1) return null;
  return { rank: index + 1, total: full.length };
}
