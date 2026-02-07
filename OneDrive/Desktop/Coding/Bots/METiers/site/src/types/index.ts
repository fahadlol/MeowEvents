// Gamemodes
export const MODES = [
  "sword",
  "axe",
  "netherite_pot",
  "diamond_pot",
  "vanilla",
  "mace",
  "uhc",
  "smp",
] as const;
export type ModeSlug = (typeof MODES)[number];

// Tiers: best → worst (HT1, LT1, HT2, LT2, HT3, LT3, HT4, LT4, HT5, LT5)
export const TIERS = [
  "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5",
] as const;
export type Tier = (typeof TIERS)[number];

// Retired and unranked (0 points, prestige only)
export const RETIRED_TIERS = [
  "RHT1", "RHT2", "RLT1", "RLT2",
] as const;
export const UNRANKED_TIER = "UNR" as const;
export const ALL_TIERS = [...TIERS, ...RETIRED_TIERS, UNRANKED_TIER] as const;
export type AnyTier = (typeof ALL_TIERS)[number];

/** Points per tier (current tier only; retired and UNR = 0). */
export const TIER_POINTS: Record<string, number> = {
  HT1: 60, HT2: 30, HT3: 10, HT4: 4, HT5: 2,
  LT1: 45, LT2: 20, LT3: 6, LT4: 3, LT5: 1,
  RHT1: 0, RHT2: 0, RLT1: 0, RLT2: 0,
  UNR: 0,
};

export function tierPoints(tier: string): number {
  return TIER_POINTS[tier] ?? 0;
}

export function isRetiredTier(tier: string): boolean {
  return tier.startsWith("RHT") || tier.startsWith("RLT");
}

export function isUnrankedTier(tier: string): boolean {
  return tier === UNRANKED_TIER;
}

export function tierCategory(tier: Tier): "HT" | "LT" {
  return tier.startsWith("HT") ? "HT" : "LT";
}

/** Lower = better (HT1=0, LT5=9). Used for leaderboard sort. */
export function tierRank(tier: Tier): number {
  const i = TIERS.indexOf(tier as Tier);
  return i === -1 ? 999 : i;
}

/** Order for peak comparison: lower = better. HT1=0, LT1=1, ..., retired/unr after LT5. */
const PEAK_ORDER: Record<string, number> = {
  HT1: 0, LT1: 1, HT2: 2, LT2: 3, HT3: 4, LT3: 5, HT4: 6, LT4: 7, HT5: 8, LT5: 9,
  RHT1: 10, RHT2: 11, RLT1: 12, RLT2: 13,
  UNR: 20,
};

export function tierRankForPeak(tier: string): number {
  return PEAK_ORDER[tier] ?? 999;
}

/** True if newTier is strictly better than currentPeak (for updating peak). */
export function isTierBetterThanPeak(newTier: string, currentPeak: string | null): boolean {
  if (!currentPeak) return true;
  return tierRankForPeak(newTier) < tierRankForPeak(currentPeak);
}

/** Tier bands for column layout: Tier 1 (best) → Tier 5 (worst). Retired/UNR map to band 5. */
export const TIER_BANDS = [
  { band: 1 as const, label: "Tier 1", tiers: ["HT1", "LT1"] as const, color: "tier-1" },
  { band: 2 as const, label: "Tier 2", tiers: ["HT2", "LT2"] as const, color: "tier-2" },
  { band: 3 as const, label: "Tier 3", tiers: ["HT3", "LT3"] as const, color: "tier-3" },
  { band: 4 as const, label: "Tier 4", tiers: ["HT4", "LT4"] as const, color: "tier-4" },
  { band: 5 as const, label: "Tier 5", tiers: ["HT5", "LT5"] as const, color: "tier-5" },
] as const;

export function tierToBand(tier: string): 1 | 2 | 3 | 4 | 5 {
  for (const b of TIER_BANDS) {
    if (b.tiers.includes(tier as any)) return b.band;
  }
  return 5;
}

/** Valid tier for points/ranking (includes retired and UNR). */
export function isValidTier(tier: string): boolean {
  return ALL_TIERS.includes(tier as AnyTier);
}

export interface Player {
  id: string;
  username: string;
  region: string;
  created_at: string;
  updated_at: string;
}

export interface PlayerTier {
  id: string;
  player_id: string;
  mode: ModeSlug;
  tier: Tier;
  last_tested_at: string;
  tester_name: string;
  created_at: string;
  updated_at: string;
}

export interface PlayerWithTiers extends Player {
  tiers: Partial<Record<ModeSlug, { tier: Tier; peak_tier?: string | null; last_tested_at: string; tester_name: string }>>;
}

export interface Mode {
  slug: ModeSlug;
  name: string;
}

export const MODE_NAMES: Record<ModeSlug, string> = {
  sword: "Sword",
  axe: "Axe",
  netherite_pot: "Netherite Pot",
  diamond_pot: "Diamond Pot",
  vanilla: "Vanilla",
  mace: "Mace",
  uhc: "UHC",
  smp: "SMP",
};
