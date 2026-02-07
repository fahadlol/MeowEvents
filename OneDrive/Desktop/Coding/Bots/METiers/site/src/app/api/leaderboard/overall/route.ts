import { NextResponse } from "next/server";
import { getOverallLeaderboardFull } from "@/lib/data";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const limit = Math.min(25, Math.max(1, parseInt(searchParams.get("limit") || "10", 10)));
  const rows = await getOverallLeaderboardFull(0);
  const top = rows.slice(0, limit);
  return NextResponse.json({ leaderboard: top });
}
