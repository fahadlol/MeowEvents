import { NextResponse } from "next/server";
import { getSupabase } from "@/lib/supabase";

export async function GET() {
  const supabase = getSupabase();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured" }, { status: 503 });
  }

  // Get all players
  const { data: players, error: playersError } = await supabase
    .from("players")
    .select("*")
    .limit(10);

  // Get all player_tiers
  const { data: tiers, error: tiersError } = await supabase
    .from("player_tiers")
    .select("*, player:players(username)")
    .limit(10);

  return NextResponse.json({
    players: { data: players, error: playersError?.message },
    tiers: { data: tiers, error: tiersError?.message },
  });
}
