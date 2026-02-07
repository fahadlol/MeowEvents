import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";

export type SyncGuild = {
  id: string;
  name: string;
  icon: string | null;
  memberCount: number;
};

export async function GET() {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const botUrl = process.env.BOT_API_URL;
  const botSecret = process.env.BOT_API_SECRET;
  if (!botUrl || !botSecret) {
    return NextResponse.json(
      {
        error:
          "Configure BOT_API_URL and BOT_API_SECRET in .env.local to list Discord servers.",
      },
      { status: 503 }
    );
  }

  const res = await fetch(`${botUrl.replace(/\/$/, "")}/api/guilds`, {
    headers: { "X-API-Secret": botSecret },
  });
  if (!res.ok) {
    const text = await res.text();
    return NextResponse.json(
      { error: `Bot API error: ${res.status} ${text}` },
      { status: 502 }
    );
  }

  const list = (await res.json()) as SyncGuild[];
  return NextResponse.json(list);
}
