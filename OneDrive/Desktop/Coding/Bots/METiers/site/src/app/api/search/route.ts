import { NextRequest, NextResponse } from "next/server";
import { getSupabase } from "@/lib/supabase";
import { MODES, MODE_NAMES, type ModeSlug } from "@/types";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const q = req.nextUrl.searchParams.get("q")?.trim();
  if (!q || q.length < 2) {
    return NextResponse.json({ players: [] });
  }

  const supabase = getSupabase();
  if (!supabase) {
    return NextResponse.json({ error: "Database not configured" }, { status: 503 });
  }

  // Search players by username
  const { data: players, error } = await supabase
    .from("players")
    .select("id, username, discord_id, region")
    .ilike("username", `%${q}%`)
    .limit(8);

  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  if (!players || players.length === 0) {
    return NextResponse.json({ players: [] });
  }

  // Get tiers for all found players
  const playerIds = players.map((p) => p.id);
  const { data: tiers } = await supabase
    .from("player_tiers")
    .select("player_id, mode, tier, tester_name, last_tested_at")
    .in("player_id", playerIds);

  // Build response with tiers grouped by player
  const results = players.map((player) => {
    const playerTiers = (tiers || []).filter((t) => t.player_id === player.id);
    const tiersByMode: Record<string, { tier: string; tester: string; date: string }> = {};

    playerTiers.forEach((t) => {
      tiersByMode[t.mode] = {
        tier: t.tier,
        tester: t.tester_name || "Unknown",
        date: t.last_tested_at,
      };
    });

    return {
      id: player.id,
      username: player.username,
      discord_id: player.discord_id,
      region: player.region,
      tiers: tiersByMode,
    };
  });

  return NextResponse.json({ players: results });
}
