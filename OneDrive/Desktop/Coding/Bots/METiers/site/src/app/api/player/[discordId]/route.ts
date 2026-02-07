import { NextResponse } from "next/server";
import { getSupabase } from "@/lib/supabase";
import { getOverallRankAndTotal } from "@/lib/data";
import { tierPoints } from "@/types";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || "";

export async function GET(
  req: Request,
  { params }: { params: Promise<{ discordId: string }> }
) {
  const { discordId } = await params;
  const supabase = getSupabase();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured" }, { status: 503 });
  }

  const { data: player, error: playerError } = await supabase
    .from("players")
    .select("id, username, region, discord_id")
    .eq("discord_id", discordId)
    .maybeSingle();

  if (playerError) return NextResponse.json({ error: playerError.message }, { status: 500 });
  if (!player) return NextResponse.json({ error: "Player not found" }, { status: 404 });

  const { data: tiers, error: tiersError } = await supabase
    .from("player_tiers")
    .select("mode, tier, peak_tier, tester_name, last_tested_at")
    .eq("player_id", player.id)
    .order("mode");

  if (tiersError) return NextResponse.json({ error: tiersError.message }, { status: 500 });

  const rankInfo = await getOverallRankAndTotal(player.id);
  const totalPoints = (tiers || []).reduce((sum, t) => sum + tierPoints(t.tier), 0);
  const profileUrl = siteUrl ? `${siteUrl}/player/${encodeURIComponent(player.username)}` : null;

  return NextResponse.json({
    player: {
      username: player.username,
      region: player.region,
      discord_id: player.discord_id,
      overall_rank: rankInfo?.rank ?? null,
      total_players: rankInfo?.total ?? null,
      total_points: totalPoints,
      profile_url: profileUrl,
    },
    tiers: tiers || [],
  });
}
