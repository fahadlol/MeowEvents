import { NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { createAdminClient } from "@/lib/supabase";
import { MODES, isValidTier, isTierBetterThanPeak } from "@/types";

const DEFAULT_MODE = "sword";

type TierEntry = {
  userId: string;
  username?: string | null;
  tier: string;
  mode: string;
  updatedAt?: number;
  updatedBy?: string;
};

const DEFAULT_REGION = "Middle East";

function sanitizeUsername(raw: string | null | undefined): string {
  if (!raw || typeof raw !== "string") return "";
  return raw.replace(/#\d+$/, "").trim().slice(0, 64) || "";
}

function resolveGuildIds(body: { guildIds?: string[] } | null): string[] {
  if (body?.guildIds && Array.isArray(body.guildIds) && body.guildIds.length > 0) {
    return body.guildIds.filter(Boolean);
  }
  const idsEnv = process.env.DISCORD_GUILD_IDS;
  if (idsEnv && idsEnv.trim()) {
    return idsEnv.split(",").map((s) => s.trim()).filter(Boolean);
  }
  const single = process.env.DISCORD_GUILD_ID;
  if (single && single.trim()) return [single.trim()];
  return [];
}

export async function POST(req: Request) {
  if (!(await isAdmin())) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const botUrl = process.env.BOT_API_URL;
  const botSecret = process.env.BOT_API_SECRET;
  if (!botUrl || !botSecret) {
    return NextResponse.json(
      {
        error:
          "Configure BOT_API_URL and BOT_API_SECRET in .env.local to sync from Discord.",
      },
      { status: 503 }
    );
  }

  let body: { guildIds?: string[] } | null = null;
  try {
    const text = await req.text();
    if (text?.trim()) body = JSON.parse(text);
  } catch {
    // no body or invalid JSON
  }

  const guildIdsToSync = resolveGuildIds(body);
  if (guildIdsToSync.length === 0) {
    return NextResponse.json(
      {
        error:
          "Set DISCORD_GUILD_ID or DISCORD_GUILD_IDS (comma-separated) in .env.local, or send guildIds in the request body.",
      },
      { status: 503 }
    );
  }

  const baseUrl = botUrl.replace(/\/$/, "");
  const mergedByUserAndMode = new Map<string, TierEntry>();

  for (const guildId of guildIdsToSync) {
    const res = await fetch(
      `${baseUrl}/api/tiers?guildId=${encodeURIComponent(guildId)}`,
      { headers: { "X-API-Secret": botSecret }, signal: AbortSignal.timeout(10000) }
    );
    if (!res.ok) {
      const text = await res.text();
      return NextResponse.json(
        { error: `Bot API error for guild ${guildId}: ${res.status} ${text}` },
        { status: 502 }
      );
    }
    const data = (await res.json()) as {
      tiers: {
        userId: string;
        username?: string | null;
        tier: string;
        mode?: string;
        updatedAt?: number;
        updatedBy?: string;
      }[];
    };
    const tiers = data.tiers || [];
    for (const t of tiers) {
      const mode = (t.mode && MODES.includes(t.mode as any)) ? t.mode : DEFAULT_MODE;
      const key = `${t.userId}:${mode}`;
      const existing = mergedByUserAndMode.get(key);
      const updatedAt = t.updatedAt ?? 0;
      if (
        !existing ||
        (typeof existing.updatedAt === "number" && updatedAt > existing.updatedAt)
      ) {
        mergedByUserAndMode.set(key, {
          userId: t.userId,
          username: t.username,
          tier: t.tier,
          mode,
          updatedAt,
          updatedBy: t.updatedBy,
        });
      }
    }
  }

  const tiers = Array.from(mergedByUserAndMode.values());

  const supabase = createAdminClient();
  if (!supabase) {
    return NextResponse.json({ error: "Supabase not configured" }, { status: 503 });
  }

  let updatedCount = 0;
  let skipped = 0;
  let createdCount = 0;
  const modesUsed = new Set<string>();

  for (const t of tiers) {
    const tierValue = t.tier?.trim();
    if (!tierValue || !isValidTier(tierValue)) {
      skipped++;
      continue;
    }
    const mode = t.mode && MODES.includes(t.mode as any) ? t.mode : DEFAULT_MODE;
    modesUsed.add(mode);

    let { data: player, error: playerError } = await supabase
      .from("players")
      .select("id")
      .eq("discord_id", t.userId)
      .maybeSingle();

    if (playerError) {
      skipped++;
      continue;
    }

    if (!player) {
      const username =
        sanitizeUsername(t.username) || `discord-${t.userId.slice(-8)}`;
      const { data: existingByUsername } = await supabase
        .from("players")
        .select("id, discord_id")
        .eq("username", username)
        .maybeSingle();

      if (existingByUsername) {
        if (!existingByUsername.discord_id) {
          const { error: linkErr } = await supabase
            .from("players")
            .update({ discord_id: t.userId })
            .eq("id", existingByUsername.id);
          if (!linkErr) player = existingByUsername;
        } else {
          player = existingByUsername;
        }
      }
      if (!player) {
        let finalUsername = username;
        const { data: newPlayer, error: createErr } = await supabase
          .from("players")
          .insert({
            username: finalUsername,
            discord_id: t.userId,
            region: DEFAULT_REGION,
          })
          .select("id")
          .single();
        if (createErr) {
          if (createErr.code === "23505" && finalUsername === username) {
            finalUsername = `${username}-${t.userId.slice(-6)}`;
            const { data: retryPlayer, error: retryErr } = await supabase
              .from("players")
              .insert({
                username: finalUsername,
                discord_id: t.userId,
                region: DEFAULT_REGION,
              })
              .select("id")
              .single();
            if (!retryErr && retryPlayer) {
              player = retryPlayer;
              createdCount++;
            } else {
              skipped++;
              continue;
            }
          } else {
            skipped++;
            continue;
          }
        } else {
          player = newPlayer;
          createdCount++;
        }
      }
    }

    if (!player) {
      skipped++;
      continue;
    }

    const { data: existing } = await supabase
      .from("player_tiers")
      .select("id, tier, peak_tier")
      .eq("player_id", player.id)
      .eq("mode", mode as string)
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
      tester_name: t.updatedBy || "Discord sync",
      last_tested_at: new Date().toISOString(),
    };

    if (existing) {
      const { error } = await supabase
        .from("player_tiers")
        .update(row)
        .eq("id", existing.id);
      if (!error) {
        updatedCount++;
        const { logTierChange } = await import("@/lib/audit");
        await logTierChange({
          player_id: player.id,
          username: player.username,
          mode: mode as string,
          old_tier: existing.tier ?? null,
          new_tier: tierValue,
          source: "sync",
          changed_by: t.updatedBy || null,
        });
      }
    } else {
      const { error } = await supabase.from("player_tiers").insert({
        player_id: player.id,
        mode,
        ...row,
      });
      if (!error) {
        updatedCount++;
        const { logTierChange } = await import("@/lib/audit");
        await logTierChange({
          player_id: player.id,
          username: player.username,
          mode: mode as string,
          old_tier: null,
          new_tier: tierValue,
          source: "sync",
          changed_by: t.updatedBy || null,
        });
      }
    }
  }

  return NextResponse.json({
    ok: true,
    updated: updatedCount,
    created: createdCount,
    skipped,
    total: tiers.length,
    guildsSynced: guildIdsToSync.length,
    modes: Array.from(modesUsed),
  });
}
