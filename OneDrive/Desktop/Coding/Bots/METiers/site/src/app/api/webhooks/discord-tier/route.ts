import { NextResponse } from "next/server";
import { timingSafeEqual } from "crypto";
import { createAdminClient } from "@/lib/supabase";
import { ALL_TIERS, MODES, isValidTier, isTierBetterThanPeak } from "@/types";

const DEFAULT_MODE = "sword";

export async function POST(req: Request) {
  const secret = req.headers.get("X-Sync-Secret") || "";
  const expected = process.env.SYNC_SECRET;
  if (!expected || secret.length !== expected.length ||
      !timingSafeEqual(Buffer.from(secret), Buffer.from(expected))) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const body = (await req.json()) as {
    userId: string;
    username?: string;
    tier: string;
    kit?: string;
    guildId?: string;
    testerTag?: string;
  };
  const { userId, username, tier, kit, testerTag } = body;
  if (!userId || !tier?.trim()) {
    return NextResponse.json({ error: "userId and tier required" }, { status: 400 });
  }

  const tierValue = tier.trim();
  if (!isValidTier(tierValue)) {
    return NextResponse.json({ error: `Invalid tier. Allowed: ${ALL_TIERS.join(", ")}` }, { status: 400 });
  }

  const supabase = createAdminClient();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured (missing SUPABASE_SERVICE_ROLE_KEY)" }, { status: 503 });
  }

  // Use kit as mode if provided and valid, otherwise use default
  let mode = kit?.toLowerCase() || process.env.SYNC_DEFAULT_MODE || DEFAULT_MODE;
  if (!MODES.includes(mode as any)) {
    mode = DEFAULT_MODE; // Fallback to default if kit doesn't match a valid mode
  }

  // Try to find existing player by discord_id
  let { data: player } = await supabase
    .from("players")
    .select("id")
    .eq("discord_id", userId)
    .maybeSingle();

  // If no player found, create one automatically
  if (!player && username) {
    // Check if username already exists
    const { data: existingByUsername } = await supabase
      .from("players")
      .select("id, discord_id")
      .eq("username", username)
      .maybeSingle();

    if (existingByUsername) {
      // Link discord_id to existing player if not already linked
      if (!existingByUsername.discord_id) {
        await supabase
          .from("players")
          .update({ discord_id: userId })
          .eq("id", existingByUsername.id);
      }
      player = existingByUsername;
    } else {
      // Create new player
      const { data: newPlayer, error: createError } = await supabase
        .from("players")
        .insert({
          username: username,
          discord_id: userId,
          region: "Middle East",
        })
        .select("id")
        .single();

      if (createError) {
        return NextResponse.json({ error: `Failed to create player: ${createError.message}` }, { status: 500 });
      }
      player = newPlayer;
    }
  }

  if (!player) {
    return NextResponse.json({ error: "No player linked to this Discord user and no username provided" }, { status: 404 });
  }

  const { data: existing } = await supabase
    .from("player_tiers")
    .select("id, tier, peak_tier")
    .eq("player_id", player.id)
    .eq("mode", mode)
    .maybeSingle();

  let newPeak: string;
  if (existing?.peak_tier != null) {
    newPeak = isTierBetterThanPeak(tierValue, existing.peak_tier) ? tierValue : existing.peak_tier;
  } else {
    const currentTier = existing?.tier ?? tierValue;
    newPeak = isTierBetterThanPeak(tierValue, currentTier) ? tierValue : currentTier;
  }

  const row = {
    tier: tierValue,
    peak_tier: newPeak,
    tester_name: testerTag || "Discord",
    last_tested_at: new Date().toISOString(),
  };

  if (existing) {
    const { error } = await supabase.from("player_tiers").update(row).eq("id", existing.id);
    if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  } else {
    const { error } = await supabase.from("player_tiers").insert({
      player_id: player.id,
      mode,
      ...row,
    });
    if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  }

  const { logTierChange } = await import("@/lib/audit");
  await logTierChange({
    player_id: player.id,
    username: player.username,
    mode,
    old_tier: existing?.tier ?? null,
    new_tier: tierValue,
    source: "webhook",
    changed_by: testerTag || null,
  });

  return NextResponse.json({ ok: true, mode, tier: tierValue });
}
