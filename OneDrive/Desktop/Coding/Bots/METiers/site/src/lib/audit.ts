import { createAdminClient } from "./supabase";

export type AuditSource = "admin" | "webhook" | "sync";

export async function logTierChange(params: {
  player_id: string;
  username: string;
  mode: string;
  old_tier: string | null;
  new_tier: string;
  source: AuditSource;
  changed_by?: string | null;
}): Promise<void> {
  const supabase = createAdminClient();
  if (!supabase) return;
  await supabase.from("tier_audit_log").insert({
    player_id: params.player_id,
    username: params.username,
    mode: params.mode,
    old_tier: params.old_tier,
    new_tier: params.new_tier,
    source: params.source,
    changed_by: params.changed_by ?? null,
  });
}
