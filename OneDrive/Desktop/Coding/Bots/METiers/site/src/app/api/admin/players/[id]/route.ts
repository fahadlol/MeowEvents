import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase";
import { logTierChange } from "@/lib/audit";
import { MODES, isValidTier, isTierBetterThanPeak, tierRankForPeak } from "@/types";

export async function PATCH(
  req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const { id } = await params;
  const body = (await req.json()) as {
    username?: string;
    region?: string;
    discord_id?: string;
    tier?: { mode: string; tier: string; last_tested_at?: string; tester_name: string };
  };
  const supabase = createAdminClient();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured" }, { status: 503 });
  }
  if (body.username !== undefined || body.region !== undefined || body.discord_id !== undefined) {
    const { error } = await supabase
      .from("players")
      .update({
        ...(body.username !== undefined && { username: body.username.trim() }),
        ...(body.region !== undefined && { region: body.region }),
        ...(body.discord_id !== undefined && { discord_id: body.discord_id?.trim() || null }),
      })
      .eq("id", id);
    if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  }

  if (body.tier && MODES.includes(body.tier.mode as any) && isValidTier(body.tier.tier)) {
    const { data: existing } = await supabase
      .from("player_tiers")
      .select("id, tier, peak_tier")
      .eq("player_id", id)
      .eq("mode", body.tier.mode)
      .maybeSingle();

    let newPeak: string;
    if (existing?.peak_tier != null) {
      const storedPeakWorseThanNew =
        tierRankForPeak(existing.peak_tier) > tierRankForPeak(body.tier.tier);
      if (storedPeakWorseThanNew) {
        newPeak = body.tier.tier;
      } else {
        newPeak = isTierBetterThanPeak(body.tier.tier, existing.peak_tier)
          ? body.tier.tier
          : existing.peak_tier;
      }
    } else {
      const currentTier = existing?.tier ?? body.tier.tier;
      newPeak = isTierBetterThanPeak(body.tier.tier, currentTier)
        ? body.tier.tier
        : currentTier;
    }

    const row = {
      tier: body.tier.tier,
      peak_tier: newPeak,
      tester_name: body.tier.tester_name || "—",
      last_tested_at: body.tier.last_tested_at || new Date().toISOString(),
    };

    if (existing) {
      const { error } = await supabase
        .from("player_tiers")
        .update(row)
        .eq("id", existing.id);
      if (error) return NextResponse.json({ error: error.message }, { status: 500 });
    } else {
      const { error } = await supabase.from("player_tiers").insert({
        player_id: id,
        mode: body.tier.mode,
        ...row,
      });
      if (error) return NextResponse.json({ error: error.message }, { status: 500 });
    }

    const { data: playerRow } = await supabase.from("players").select("username").eq("id", id).single();
    await logTierChange({
      player_id: id,
      username: playerRow?.username ?? "Unknown",
      mode: body.tier.mode,
      old_tier: existing?.tier ?? null,
      new_tier: body.tier.tier,
      source: "admin",
      changed_by: body.tier.tester_name || null,
    });
  }

  return NextResponse.json({ ok: true });
}
