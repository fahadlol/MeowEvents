import { NextResponse } from "next/server";
import { getLeaderboard } from "@/lib/data";
import { MODES } from "@/types";

export async function GET(
  req: Request,
  { params }: { params: Promise<{ slug: string }> }
) {
  const { slug } = await params;
  if (!MODES.includes(slug as any)) {
    return NextResponse.json({ error: "Invalid mode" }, { status: 400 });
  }
  const { searchParams } = new URL(req.url);
  const limit = Math.min(25, Math.max(1, parseInt(searchParams.get("limit") || "10", 10)));
  const rows = await getLeaderboard(slug as any);
  const top = rows.slice(0, limit);
  return NextResponse.json({ leaderboard: top });
}
