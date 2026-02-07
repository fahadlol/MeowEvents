import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase";
import { MODES, isValidTier } from "@/types";

export async function GET() {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const supabase = createAdminClient();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured (SUPABASE_SERVICE_ROLE_KEY, NEXT_PUBLIC_SUPABASE_URL)" }, { status: 503 });
  }
  const { data, error } = await supabase
    .from("players")
    .select("*, player_tiers(mode, tier, last_tested_at, tester_name)")
    .order("username");
  if (error) return NextResponse.json({ error: error.message }, { status: 500 });
  return NextResponse.json(data);
}

export async function POST(req: Request) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const body = (await req.json()) as {
    username: string;
    region?: string;
    discord_id?: string;
    tiers?: { mode: string; tier: string; tester_name: string }[];
  };
  const username = body.username?.trim();
  if (!username) {
    return NextResponse.json({ error: "username required" }, { status: 400 });
  }
  const supabase = createAdminClient();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured" }, { status: 503 });
  }
  const { data: existing } = await supabase
    .from("players")
    .select("id")
    .eq("username", username)
    .single();
  if (existing) {
    return NextResponse.json({ error: "Player already exists" }, { status: 400 });
  }

  const { data: player, error: playerError } = await supabase
    .from("players")
    .insert({
      username,
      region: body.region || "Middle East",
      discord_id: body.discord_id?.trim() || null,
    })
    .select("id")
    .single();
  if (playerError) return NextResponse.json({ error: playerError.message }, { status: 500 });
  if (!player) return NextResponse.json({ error: "Insert failed" }, { status: 500 });

  const tiers = body.tiers || [];
  for (const t of tiers) {
    if (!MODES.includes(t.mode as any) || !isValidTier(t.tier)) continue;
    await supabase.from("player_tiers").insert({
      player_id: player.id,
      mode: t.mode,
      tier: t.tier,
      peak_tier: t.tier,
      tester_name: t.tester_name || "—",
    });
  }

  return NextResponse.json({ ok: true, id: player.id });
}
