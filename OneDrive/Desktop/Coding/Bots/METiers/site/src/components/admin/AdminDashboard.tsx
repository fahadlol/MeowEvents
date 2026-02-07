"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AdminAddPlayer } from "./AdminAddPlayer";
import { AdminEditPlayer } from "./AdminEditPlayer";
import { TierBadge } from "@/components/TierBadge";
import { MODE_NAMES } from "@/types";

type PlayerRow = {
  id: string;
  username: string;
  region: string;
  discord_id?: string | null;
  player_tiers: { mode: string; tier: string; last_tested_at: string; tester_name: string }[];
};

type SyncGuild = { id: string; name: string; icon: string | null; memberCount: number };

type AuditEntry = {
  id: string;
  player_id: string;
  username: string;
  mode: string;
  old_tier: string | null;
  new_tier: string;
  source: string;
  changed_by: string | null;
  created_at: string;
};

function formatRelativeTime(date: Date): string {
  const sec = Math.floor((Date.now() - date.getTime()) / 1000);
  if (sec < 60) return "just now";
  if (sec < 3600) return `${Math.floor(sec / 60)} min ago`;
  if (sec < 86400) return `${Math.floor(sec / 3600)} hr ago`;
  return `${Math.floor(sec / 86400)} day(s) ago`;
}

type Props = { onLogout: () => void };

export function AdminDashboard({ onLogout }: Props) {
  const [players, setPlayers] = useState<PlayerRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<PlayerRow | null>(null);
  const [showAdd, setShowAdd] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);
  const [guilds, setGuilds] = useState<SyncGuild[]>([]);
  const [selectedGuildIds, setSelectedGuildIds] = useState<Set<string>>(new Set());
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null);
  const [showAuditLog, setShowAuditLog] = useState(false);
  const [auditLog, setAuditLog] = useState<AuditEntry[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);

  async function load() {
    const res = await fetch("/api/admin/players");
    if (res.ok) {
      const data = await res.json();
      setPlayers(data);
    }
    setLoading(false);
  }

  async function loadGuilds() {
    const res = await fetch("/api/sync/guilds");
    if (res.ok) {
      const data = await res.json();
      setGuilds(data);
      setSelectedGuildIds((prev) => {
        const next = new Set(prev);
        data.forEach((g: SyncGuild) => next.add(g.id));
        return next;
      });
    }
  }

  useEffect(() => {
    load();
    loadGuilds();
  }, []);

  async function logout() {
    await fetch("/api/admin/logout", { method: "POST" });
    onLogout();
  }

  function toggleGuild(id: string) {
    setSelectedGuildIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function loadAuditLog() {
    setAuditLoading(true);
    const res = await fetch("/api/admin/audit?limit=100");
    if (res.ok) {
      const data = await res.json();
      setAuditLog(data);
    }
    setAuditLoading(false);
  }

  function openAuditLog() {
    setShowAuditLog(true);
    loadAuditLog();
  }

  async function syncFromDiscord() {
    setSyncing(true);
    setSyncMessage(null);
    try {
      const body =
        selectedGuildIds.size > 0
          ? JSON.stringify({ guildIds: Array.from(selectedGuildIds) })
          : undefined;
      const res = await fetch("/api/sync/discord", {
        method: "POST",
        headers: body ? { "Content-Type": "application/json" } : undefined,
        body,
      });
      const data = await res.json();
      if (res.ok) {
        setLastSyncedAt(new Date());
        const guildNote =
          data.guildsSynced > 1 ? ` (${data.guildsSynced} servers)` : "";
        const createdNote =
          data.created > 0 ? ` ${data.created} player(s) created.` : "";
        const modeNote =
          data.modes?.length > 0
            ? ` Modes: ${(data.modes as string[]).join(", ")}.`
            : "";
        setSyncMessage(
          `Synced${guildNote}: ${data.updated} updated,${createdNote} ${data.skipped} skipped.${modeNote}`
        );
        load();
      } else {
        setSyncMessage(data.error || "Sync failed");
      }
    } catch {
      setSyncMessage("Sync request failed");
    }
    setSyncing(false);
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <div className="mb-8 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin</h1>
        <button
          onClick={logout}
          className="rounded border border-[var(--border)] px-3 py-1.5 text-sm hover:bg-white/5"
        >
          Log out
        </button>
      </div>

      <p className="mb-6 text-[var(--muted)]">
        Add player, edit tier, assign tester. Changes are manual.
      </p>

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <button
          onClick={() => { setShowAdd(true); setEditing(null); }}
          className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-500"
        >
          + Add player
        </button>
        <button
          onClick={openAuditLog}
          className="rounded border border-[var(--border)] bg-[var(--card)] px-4 py-2 text-sm font-medium hover:bg-white/5"
        >
          Audit log
        </button>
        <button
          onClick={syncFromDiscord}
          disabled={syncing}
          className="rounded border border-[var(--border)] bg-[var(--card)] px-4 py-2 text-sm font-medium hover:bg-white/5 disabled:opacity-50"
        >
          {syncing ? "Syncing…" : "Sync from Discord"}
        </button>
        {syncMessage && (
          <span className="text-sm text-[var(--muted)]">{syncMessage}</span>
        )}
        {lastSyncedAt && (
          <span className="text-xs text-[var(--muted)]">
            Last synced: {formatRelativeTime(lastSyncedAt)}
          </span>
        )}
      </div>
      {guilds.length > 0 && (
        <div className="mb-4 rounded border border-[var(--border)] bg-[var(--card)] p-3">
          <p className="mb-2 text-xs font-medium text-[var(--muted)]">
            Sync from these Discord servers (select one or more; if none selected, env guild IDs are used):
          </p>
          <div className="flex flex-wrap gap-2">
            {guilds.map((g) => (
              <label
                key={g.id}
                className="flex cursor-pointer items-center gap-2 rounded border border-[var(--border)] px-3 py-1.5 text-sm hover:bg-white/5"
              >
                <input
                  type="checkbox"
                  checked={selectedGuildIds.has(g.id)}
                  onChange={() => toggleGuild(g.id)}
                  className="rounded"
                />
                {g.icon && (
                  <img
                    src={g.icon}
                    alt=""
                    className="h-5 w-5 rounded-full"
                  />
                )}
                <span>{g.name}</span>
                <span className="text-xs text-[var(--muted)]">({g.memberCount})</span>
              </label>
            ))}
          </div>
        </div>
      )}
      <p className="mb-6 text-xs text-[var(--text-muted)]">
        Sync from Discord: set <code className="rounded bg-black/30 px-1">BOT_API_URL</code> and <code className="rounded bg-black/30 px-1">BOT_API_SECRET</code> in <code className="rounded bg-black/30 px-1">site/.env.local</code>. Use <code className="rounded bg-black/30 px-1">DISCORD_GUILD_ID</code> (one) or <code className="rounded bg-black/30 px-1">DISCORD_GUILD_IDS</code> (comma-separated) for default servers, or pick servers above.
      </p>

      {showAdd && (
        <AdminAddPlayer
          onClose={() => setShowAdd(false)}
          onSaved={() => { setShowAdd(false); load(); }}
        />
      )}

      {editing && (
        <AdminEditPlayer
          player={editing}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); load(); }}
        />
      )}

      {showAuditLog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={() => setShowAuditLog(false)}>
          <div className="max-h-[80vh] w-full max-w-3xl overflow-auto rounded-xl border border-[var(--border)] bg-[var(--card)] p-4 shadow-xl" onClick={(e) => e.stopPropagation()}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold">Tier audit log</h2>
              <button onClick={() => setShowAuditLog(false)} className="rounded border border-[var(--border)] px-3 py-1 text-sm hover:bg-white/5">Close</button>
            </div>
            {auditLoading ? (
              <p className="text-[var(--muted)]">Loading…</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-[var(--border)]">
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">Time</th>
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">Player</th>
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">Mode</th>
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">Change</th>
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">Source</th>
                      <th className="px-2 py-2 font-medium text-[var(--text-muted)]">By</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLog.map((e) => (
                      <tr key={e.id} className="border-b border-[var(--border)]/50">
                        <td className="whitespace-nowrap px-2 py-2 text-[var(--text-muted)]">{new Date(e.created_at).toLocaleString()}</td>
                        <td className="px-2 py-2">
                          <Link href={`/player/${encodeURIComponent(e.username)}`} className="text-[var(--accent)] hover:underline">{e.username}</Link>
                        </td>
                        <td className="px-2 py-2 text-[var(--text-muted)]">{MODE_NAMES[e.mode as keyof typeof MODE_NAMES] ?? e.mode}</td>
                        <td className="px-2 py-2">{e.old_tier ?? "—"} → {e.new_tier}</td>
                        <td className="px-2 py-2 text-[var(--text-muted)]">{e.source}</td>
                        <td className="px-2 py-2 text-[var(--text-muted)]">{e.changed_by ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {auditLog.length === 0 && !auditLoading && <p className="py-4 text-center text-[var(--muted)]">No tier changes logged yet.</p>}
              </div>
            )}
          </div>
        </div>
      )}

      {loading ? (
        <p className="text-[var(--muted)]">Loading players…</p>
      ) : (
        <div className="overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--card)]">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-[var(--border)] bg-black/20">
                <th className="px-4 py-3 font-medium">Player</th>
                <th className="px-4 py-3 font-medium">Region</th>
                <th className="px-4 py-3 font-medium">Discord ID</th>
                <th className="px-4 py-3 font-medium">Tiers</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {players.map((p) => (
                <tr
                  key={p.id}
                  className="border-b border-[var(--border)]/50 hover:bg-white/5"
                >
                  <td className="px-4 py-3">
                    <Link
                      href={`/player/${encodeURIComponent(p.username)}`}
                      className="text-[var(--accent)] hover:underline"
                    >
                      {p.username}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-[var(--muted)]">{p.region}</td>
                  <td className="px-4 py-3 font-mono text-xs text-[var(--muted)]">{p.discord_id ?? "—"}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      {(p.player_tiers || []).map((t) => (
                        <span key={t.mode} className="flex items-center gap-1 text-sm">
                          <span className="text-[var(--muted)]">{MODE_NAMES[t.mode as keyof typeof MODE_NAMES]}:</span>
                          <TierBadge tier={t.tier} />
                        </span>
                      ))}
                      {(p.player_tiers || []).length === 0 && (
                        <span className="text-[var(--muted)]">—</span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => { setEditing(p); setShowAdd(false); }}
                      className="text-sm text-[var(--accent)] hover:underline"
                    >
                      Edit
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {players.length === 0 && (
            <p className="p-8 text-center text-[var(--muted)]">No players yet. Add one above.</p>
          )}
        </div>
      )}
    </div>
  );
}
